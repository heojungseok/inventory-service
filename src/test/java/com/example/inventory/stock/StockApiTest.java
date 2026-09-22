package com.example.inventory.stock;

import com.example.inventory.global.response.ErrorResponse;
import com.example.inventory.stock.in.InboundRequest;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StockApiTest extends IntegrationTest {

    private static final String INBOUND_URL = "/api/v1/stocks/inbound";

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
}
