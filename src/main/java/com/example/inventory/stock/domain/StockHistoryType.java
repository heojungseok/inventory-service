package com.example.inventory.stock.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StockHistoryType {

    INBOUND,
    OUTBOUND;

}
