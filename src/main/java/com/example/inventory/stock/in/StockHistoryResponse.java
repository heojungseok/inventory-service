package com.example.inventory.stock.in;

import com.example.inventory.stock.domain.StockHistory;
import com.example.inventory.stock.domain.StockHistoryType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class StockHistoryResponse {

    private StockHistoryType type;
    private int quantity;
    private int quantityAfter;
    private Instant createdAt;

    public static StockHistoryResponse from(StockHistory history) {
        return new StockHistoryResponse(
                history.getType(),
                history.getQuantity(),
                history.getQuantityAfter(),
                history.getCreatedAt()
        );
    }

}
