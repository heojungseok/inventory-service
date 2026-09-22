package com.example.inventory.stock.in;

import com.example.inventory.stock.app.StockQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {
    private final StockQueryUseCase stockQueryUseCase;

    @GetMapping("/products/{id}/stock")
    public void getProduct(@PathVariable Long id) {
        stockQueryUseCase.findByProductId(id);
    }
}
