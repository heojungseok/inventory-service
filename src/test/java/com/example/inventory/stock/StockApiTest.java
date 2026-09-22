package com.example.inventory.stock;

import com.example.inventory.support.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;

@Slf4j
public class StockApiTest extends IntegrationTest {

    @Test
    void 재고_조회_없는_상품이면_404() {
        ResponseEntity<String> entity = testRestTemplate.getForEntity("/api/v1/products/999/stock", String.class);
        log.debug("status: {}", entity.getStatusCode());

        assertThat(entity.getBody()).contains("PRODUCT_NOT_FOUND");

    }
}
