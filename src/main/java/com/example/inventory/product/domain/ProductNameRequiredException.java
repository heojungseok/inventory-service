package com.example.inventory.product.domain;

/** 처음 보는 sku로 입고할 때 상품명이 없으면 상품을 등록할 수 없다 */
public class ProductNameRequiredException extends RuntimeException {
    public ProductNameRequiredException() {
        super("신규 상품은 상품명이 필요합니다.");
    }
}
