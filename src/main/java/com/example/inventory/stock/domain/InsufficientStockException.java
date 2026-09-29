package com.example.inventory.stock.domain;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException() {
        super("재고가 부족합니다.");
    }
}
