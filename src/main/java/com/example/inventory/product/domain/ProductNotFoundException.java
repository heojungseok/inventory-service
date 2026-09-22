package com.example.inventory.product.domain;

import com.example.inventory.global.exception.BusinessException;
import com.example.inventory.global.exception.ErrorCode;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException() {
        super(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
