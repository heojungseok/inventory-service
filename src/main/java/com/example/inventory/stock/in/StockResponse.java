package com.example.inventory.stock.in;

import com.example.inventory.stock.domain.Stock;
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
}
