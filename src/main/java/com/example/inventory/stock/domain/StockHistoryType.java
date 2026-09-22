package com.example.inventory.stock.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StockHistoryType {

    INBOUND("입고"),
    OUTBOUND("출고");

    private final String message;
}
