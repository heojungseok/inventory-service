package com.example.inventory.stock;

import com.example.inventory.global.response.ErrorResponse;
import com.example.inventory.stock.domain.StockHistoryType;
import com.example.inventory.stock.in.InboundRequest;
import com.example.inventory.stock.in.OutboundRequest;
import com.example.inventory.stock.in.StockHistoryResponse;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.support.IntegrationTest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StockApiTest extends IntegrationTest {

    private static final String INBOUND_URL = "/api/v1/stocks/inbound";
    private static final String OUTBOUND_URL = "/api/v1/stocks/outbound";

    // 테스트마다 고유한 sku를 써서 테스트 간 데이터가 섞이지 않게 한다 (@Transactional 롤백은 동시성 테스트와 충돌하므로 쓰지 않음)
    private static String uniqueSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void 재고_조회_없는_상품이면_404() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.getForEntity("/api/v1/products/999/stock", ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody().getCode()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void 미등록_sku로_입고하면_상품이_등록되고_수량이_반영된다() {
        String sku = uniqueSku();

        ResponseEntity<StockResponse> inbound = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "상품 A", 10), StockResponse.class);

        assertThat(inbound.getStatusCode()).isEqualTo(HttpStatus.OK);
        StockResponse body = inbound.getBody();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getSku()).isEqualTo(sku);
        assertThat(body.getName()).isEqualTo("상품 A");
        assertThat(body.getQuantity()).isEqualTo(10);

        // 검증은 DB가 아니라 재고 조회 API로 한다 (HTTP seam)
        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + body.getId() + "/stock", StockResponse.class);

        assertThat(query.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(query.getBody().getQuantity()).isEqualTo(10);
    }

    @Test
    void 기존_sku로_입고하면_수량이_누적된다() {
        String sku = uniqueSku();
        testRestTemplate.postForEntity(INBOUND_URL, new InboundRequest(sku, "상품 B", 10), StockResponse.class);

        // 두 번째 입고는 name을 보내지 않는다. 기존 상품이므로 name은 필요 없다.
        ResponseEntity<StockResponse> second = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, null, 5), StockResponse.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody().getName()).isEqualTo("상품 B");
        assertThat(second.getBody().getQuantity()).isEqualTo(15);
    }

    @Test
    void 미등록_sku인데_name이_없으면_400() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(uniqueSku(), null, 10), ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 입고_수량이_0이면_400() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(uniqueSku(), "상품 C", 0), ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 입고_name이_100자를_넘으면_400() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(uniqueSku(), "가".repeat(101), 10), ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 출고하면_수량이_감소한다() {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "상품 D", 10), StockResponse.class).getBody();

        ResponseEntity<StockResponse> outbound = testRestTemplate.postForEntity(
                OUTBOUND_URL, new OutboundRequest(sku, 3), StockResponse.class);

        assertThat(outbound.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(outbound.getBody().getQuantity()).isEqualTo(7);

        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stocked.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(7);
    }

    @Test
    void 재고보다_많이_출고하면_409이고_수량은_그대로다() {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "상품 E", 10), StockResponse.class).getBody();

        ResponseEntity<ErrorResponse> outbound = testRestTemplate.postForEntity(
                OUTBOUND_URL, new OutboundRequest(sku, 11), ErrorResponse.class);

        assertThat(outbound.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(outbound.getBody().getCode()).isEqualTo("INSUFFICIENT_STOCK");

        // 실패한 출고는 아무것도 바꾸지 않아야 한다 (트랜잭션 롤백)
        ResponseEntity<StockResponse> query = testRestTemplate.getForEntity(
                "/api/v1/products/" + stocked.getId() + "/stock", StockResponse.class);
        assertThat(query.getBody().getQuantity()).isEqualTo(10);
    }

    @Test
    void 없는_sku로_출고하면_404() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.postForEntity(
                OUTBOUND_URL, new OutboundRequest(uniqueSku(), 1), ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody().getCode()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void 출고_수량이_0이면_400() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.postForEntity(
                OUTBOUND_URL, new OutboundRequest(uniqueSku(), 0), ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void 이력은_최신순으로_페이지_단위로_조회된다() {
        String sku = uniqueSku();
        StockResponse stocked = testRestTemplate.postForEntity(
                INBOUND_URL, new InboundRequest(sku, "상품 F", 10), StockResponse.class).getBody();
        testRestTemplate.postForEntity(INBOUND_URL, new InboundRequest(sku, null, 5), StockResponse.class);
        testRestTemplate.postForEntity(OUTBOUND_URL, new OutboundRequest(sku, 3), StockResponse.class);
        String historiesUrl = "/api/v1/products/" + stocked.getId() + "/stock/histories";

        // 첫 페이지: 가장 최근 2건 (출고 3 → 입고 5)
        ResponseEntity<HistoryPage> first = testRestTemplate.getForEntity(
                historiesUrl + "?page=0&size=2", HistoryPage.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<StockHistoryResponse> firstContent = first.getBody().getContent();
        assertThat(firstContent).extracting(StockHistoryResponse::getType)
                .containsExactly(StockHistoryType.OUTBOUND, StockHistoryType.INBOUND);
        assertThat(firstContent).extracting(StockHistoryResponse::getQuantity).containsExactly(3, 5);
        assertThat(firstContent).extracting(StockHistoryResponse::getQuantityAfter).containsExactly(12, 15);
        assertThat(first.getBody().getPage().getTotalElements()).isEqualTo(3);

        // 둘째 페이지: 남은 1건 (최초 입고 10)
        ResponseEntity<HistoryPage> second = testRestTemplate.getForEntity(
                historiesUrl + "?page=1&size=2", HistoryPage.class);

        List<StockHistoryResponse> secondContent = second.getBody().getContent();
        assertThat(secondContent).extracting(StockHistoryResponse::getType).containsExactly(StockHistoryType.INBOUND);
        assertThat(secondContent).extracting(StockHistoryResponse::getQuantityAfter).containsExactly(10);
    }

    @Test
    void 없는_상품의_이력을_조회하면_404() {
        ResponseEntity<ErrorResponse> entity = testRestTemplate.getForEntity(
                "/api/v1/products/999999/stock/histories", ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody().getCode()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    /**
     * 이력 조회 응답을 읽기 위한 테스트 전용 클래스.
     * 운영 코드는 Page를 그대로 반환하고, 스프링이 {"content": [...], "page": {...}} 형식의 JSON을 만든다.
     */
    @Getter
    @NoArgsConstructor
    static class HistoryPage {
        private List<StockHistoryResponse> content;
        private PageInfo page;
    }

    @Getter
    @NoArgsConstructor
    static class PageInfo {
        private int totalElements;
    }
}
