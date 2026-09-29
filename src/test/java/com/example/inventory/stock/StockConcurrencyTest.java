package com.example.inventory.stock;

import com.example.inventory.global.response.ErrorResponse;
import com.example.inventory.stock.in.InboundRequest;
import com.example.inventory.stock.in.OutboundRequest;
import com.example.inventory.stock.app.StockResponse;
import com.example.inventory.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비관적 락이 실제로 정합성을 지키는지 HTTP seam에서 증명한다.
 * 요청은 전부 실제 HTTP로 보내고, 검증은 재고 조회 API로 한다.
 */
public class StockConcurrencyTest extends IntegrationTest {

    private static final String INBOUND_URL = "/api/v1/stocks/inbound";
    private static final String OUTBOUND_URL = "/api/v1/stocks/outbound";

    @Autowired
    private DataSource dataSource;

    private static String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void 재고_100에_동시_출고_150건이면_100건만_성공하고_잔량은_0이다() throws Exception {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "동시 출고 상품", 100), StockResponse.class).getBody();

        List<HttpStatus> results = runConcurrently(150, () ->
                (HttpStatus) testRestTemplate.postForEntity(
                        OUTBOUND_URL, new OutboundRequest(sku, 1), String.class).getStatusCode());

        long success = results.stream().filter(s -> s == HttpStatus.OK).count();
        long conflict = results.stream().filter(s -> s == HttpStatus.CONFLICT).count();

        // 기대값은 독립적으로 아는 리터럴: 재고 100개 → 성공 100, 부족 50, 잔량 0
        assertThat(success).isEqualTo(100);
        assertThat(conflict).isEqualTo(50);

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stocked.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(0);
    }

    @Test
    void 미등록_sku_동시_입고_20건이면_상품은_하나이고_수량은_20이다() throws Exception {
        String sku = uniqueSku();

        List<StockResponse> results = runConcurrently(20, () -> {
            ResponseEntity<StockResponse> entity = testRestTemplate.postForEntity(
                    INBOUND_URL, new InboundRequest(sku, "동시 입고 상품", 1), StockResponse.class);
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            return entity.getBody();
        });

        // 20개 응답의 id가 전부 같아야 상품이 하나만 만들어진 것이다
        assertThat(results).extracting(StockResponse::getId).containsOnly(results.get(0).getId());

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + results.get(0).getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(20);
    }

    @Test
    void 같은_멱등_키로_동시에_출고_10건을_보내면_재고는_한_번만_줄어든다() throws Exception {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "멱등 출고 상품", 100), StockResponse.class).getBody();
        String key = UUID.randomUUID().toString();

        List<StockResponse> results = runConcurrently(10, () -> {
            ResponseEntity<StockResponse> entity = testRestTemplate.postForEntity(
                    OUTBOUND_URL, withIdempotencyKey(new OutboundRequest(sku, 1), key), StockResponse.class);
            assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
            return entity.getBody();
        });

        // 10건 모두 같은 결과(99)를 받는다. 재시도가 동시에 몰려도 한 번만 반영된다
        assertThat(results).extracting(StockResponse::getQuantity).containsOnly(99);

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stocked.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(99);
    }

    @Test
    void 다른_트랜잭션이_재고_행을_잡고_있으면_출고는_무한정_기다리지_않고_503을_돌려준다() throws Exception {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "잠금 대기 상품", 10), StockResponse.class).getBody();

        // 다른 프로세스가 같은 재고 행을 잡고 놓지 않는 상황을 만든다
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            lockStockRow(holder, sku);

            ExecutorService pool = Executors.newSingleThreadExecutor();
            Future<ResponseEntity<ErrorResponse>> outbound = pool.submit(() -> testRestTemplate.postForEntity(
                    OUTBOUND_URL, new OutboundRequest(sku, 1), ErrorResponse.class));

            // 락 대기 상한(3초)이 없으면 이 get이 10초를 넘겨 실패한다
            ResponseEntity<ErrorResponse> response = outbound.get(10, TimeUnit.SECONDS);
            holder.rollback();
            pool.shutdown();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody().getCode()).isEqualTo("LOCK_TIMEOUT");
        }

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stocked.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(10);
    }

    @Test
    void 다른_상품에_같은_멱등_키가_동시에_쓰이면_늦은_요청은_409이고_재고는_그대로다() throws Exception {
        String skuA = uniqueSku();
        String skuB = uniqueSku();
        StockResponse stockA = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(skuA, "멱등 충돌 상품 A", 10), StockResponse.class).getBody();
        testRestTemplate.postForEntity(INBOUND_URL, new InboundRequest(skuB, "멱등 충돌 상품 B", 10), StockResponse.class);
        String key = UUID.randomUUID().toString();

        // 상품 B 요청이 같은 키로 이력을 쓰고 아직 커밋하지 않은 순간을 만든다
        try (Connection other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            insertHistoryWithKey(other, skuB, key);

            ExecutorService pool = Executors.newSingleThreadExecutor();
            Future<ResponseEntity<ErrorResponse>> outbound = pool.submit(() -> testRestTemplate.postForEntity(
                    OUTBOUND_URL, withIdempotencyKey(new OutboundRequest(skuA, 1), key), ErrorResponse.class));

            // 상품 A 요청은 키 조회를 통과한 뒤(B의 이력이 아직 안 보임) 이력 INSERT에서 B의 커밋을 기다린다
            awaitInsertWaitingOnLock();
            other.commit();

            ResponseEntity<ErrorResponse> response = outbound.get(10, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getCode()).isEqualTo("IDEMPOTENCY_CONFLICT");
        }

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stockA.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(10);
    }

    private void insertHistoryWithKey(Connection connection, String sku, String key) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into stock_history (stock_id, type, quantity, quantity_after, idempotency_key)
                select s.id, 'OUTBOUND', 1, s.quantity - 1, ? from stock s join product p on p.id = s.product_id where p.sku = ?
                """)) {
            statement.setString(1, key);
            statement.setString(2, sku);
            statement.executeUpdate();
        }
    }

    private void awaitInsertWaitingOnLock() throws Exception {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            try (Connection monitor = dataSource.getConnection();
                 PreparedStatement statement = monitor.prepareStatement("""
                         select count(*) from pg_stat_activity
                         where wait_event_type = 'Lock' and query ilike '%insert%stock_history%'
                         """);
                 ResultSet rs = statement.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("출고 요청이 이력 INSERT에서 대기하지 않았습니다");
    }

    private void lockStockRow(Connection connection, String sku) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select s.id from stock s join product p on p.id = s.product_id where p.sku = ? for update of s")) {
            statement.setString(1, sku);
            statement.executeQuery();
        }
    }

    /**
     * count개의 작업을 스레드 풀에 올리고, 모두 준비된 뒤 한 번에 출발시킨다.
     * CountDownLatch가 없으면 앞 요청이 끝난 뒤 뒤 요청이 시작돼 동시성이 생기지 않는다.
     */
    private <T> List<T> runConcurrently(int count, Supplier<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                return task.get();
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }
}
