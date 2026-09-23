package com.example.inventory.stock;

import com.example.inventory.stock.in.InboundRequest;
import com.example.inventory.stock.in.OutboundRequest;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
