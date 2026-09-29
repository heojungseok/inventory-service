package com.example.inventory.product.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void 상품명이_없으면_상품을_만들_수_없다() {
        assertThatThrownBy(() -> new Product("SKU-A001", null))
                .isInstanceOf(ProductNameRequiredException.class);
        assertThatThrownBy(() -> new Product("SKU-A001", " "))
                .isInstanceOf(ProductNameRequiredException.class);
    }
}
