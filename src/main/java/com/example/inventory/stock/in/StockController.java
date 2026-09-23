package com.example.inventory.stock.in;

import com.example.inventory.stock.app.StockInboundUseCase;
import com.example.inventory.stock.app.StockOutboundUseCase;
import com.example.inventory.stock.app.StockQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {
    private final StockQueryUseCase stockQueryUseCase;
    private final StockInboundUseCase stockInboundUseCase;
    private final StockOutboundUseCase stockOutboundUseCase;

    @GetMapping("/products/{id}/stock")
    public StockResponse getStock(@PathVariable Long id) {
        return stockQueryUseCase.findByProductId(id);
    }

    @PostMapping("/stocks/inbound")
    public StockResponse inboundRequest(@Valid @RequestBody InboundRequest request) {
        return stockInboundUseCase.inbound(
                request.getSku(),
                request.getName(),
                request.getQuantity()
        );
    }

    @PostMapping("/stocks/outbound")
    public StockResponse outboundRequest(@Valid @RequestBody OutboundRequest request) {
        return stockOutboundUseCase.outbound(
                request.getSku(),
                request.getQuantity()
        );
    }
}
