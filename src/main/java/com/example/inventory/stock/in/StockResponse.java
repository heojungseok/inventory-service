package com.example.inventory.stock.in;

import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.domain.StockHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StockResponse {

    private Long id;
    private String sku;
    private String name;
    private int quantity;

    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getProduct().getId(),
                stock.getProduct().getSku(),
                stock.getProduct().getName(),
                stock.getQuantity()
        );
    }

    public static StockResponse fromHistory(StockHistory history) {
        return new StockResponse(
                history.getStock().getProduct().getId(),
                history.getStock().getProduct().getSku(),
                history.getStock().getProduct().getName(),
                history.getQuantityAfter()
        );
    }
}
