package com.example.inventory.stock;

import com.example.inventory.global.response.ErrorResponse;
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
        ResponseEntity<ErrorResponse> entity = testRestTemplate.getForEntity("/api/v1/products/999/stock", ErrorResponse.class);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody().getCode()).isEqualTo("PRODUCT_NOT_FOUND");
    }
}
