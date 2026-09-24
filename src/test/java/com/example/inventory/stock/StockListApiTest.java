package com.example.inventory.stock;

import com.example.inventory.stock.in.InboundRequest;
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

public class StockListApiTest extends IntegrationTest {

    @Test
    void 재고_목록은_sku_순으로_한_페이지에_20개씩_조회된다() {
        // 숫자로 시작하는 sku는 다른 테스트의 "SKU-..."보다 앞에 정렬되어 첫 페이지 맨 앞에 온다
        String prefix = "000-" + UUID.randomUUID().toString().substring(0, 8);
        // sku 순서와 다르게 입고한다
        inbound(prefix + "-b", 2);
        inbound(prefix + "-a", 1);
        inbound(prefix + "-c", 3);

        // size=5를 보내도 무시하고 20개로 고정한다
        ResponseEntity<StockPage> response = testRestTemplate.getForEntity(
                "/api/v1/stocks?page=0&size=5", StockPage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<StockResponse> content = response.getBody().getContent();
        assertThat(content).extracting(StockResponse::getSku)
                .startsWith(prefix + "-a", prefix + "-b", prefix + "-c");
        assertThat(content).extracting(StockResponse::getQuantity).startsWith(1, 2, 3);
        assertThat(response.getBody().getPage().getSize()).isEqualTo(20);

        // 데이터보다 뒤 페이지는 빈 목록이다. 이때 전체 건수는 countQuery로 따로 센다
        ResponseEntity<StockPage> beyond = testRestTemplate.getForEntity("/api/v1/stocks?page=1000", StockPage.class);
        assertThat(beyond.getBody().getContent()).isEmpty();
        assertThat(beyond.getBody().getPage().getTotalPages()).isPositive();
    }

    private void inbound(String sku, int quantity) {
        testRestTemplate.postForEntity(
                "/api/v1/stocks/inbound", new InboundRequest(sku, "목록 상품", quantity), StockResponse.class);
    }

    /** 재고 목록 응답({"content": [...], "page": {...}})을 읽기 위한 테스트 전용 클래스 */
    @Getter
    @NoArgsConstructor
    static class StockPage {
        private List<StockResponse> content;
        private PageInfo page;
    }

    @Getter
    @NoArgsConstructor
    static class PageInfo {
        private int size;
        private int totalPages;
    }
}
