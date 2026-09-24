package com.example.inventory.stock.app;

import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.stock.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockQueryUseCase {
    private static final int PAGE_SIZE = 20;

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResponse findByProductId(Long productId) {

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(ProductNotFoundException::new);

        return StockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    public Page<StockResponse> findStocks(int page) {
        return stockRepository.findAllWithProduct(PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(StockResponse::from);
    }
}
