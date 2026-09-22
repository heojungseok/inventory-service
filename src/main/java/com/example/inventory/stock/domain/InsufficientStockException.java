package com.example.inventory.stock.domain;

import com.example.inventory.global.exception.BusinessException;
import com.example.inventory.global.exception.ErrorCode;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException() {
        super(ErrorCode.INSUFFICIENT_STOCK);
    }
}
