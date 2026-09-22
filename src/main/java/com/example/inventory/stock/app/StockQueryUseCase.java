package com.example.inventory.stock.app;

import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.product.out.ProductRepository;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.stock.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockQueryUseCase {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResponse findByProductId(Long productId) {

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(ProductNotFoundException::new);

        return StockResponse.from(stock);
    }

}
