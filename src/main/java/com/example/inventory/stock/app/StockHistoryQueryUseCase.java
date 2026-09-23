package com.example.inventory.stock.app;

import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.domain.StockHistory;
import com.example.inventory.stock.in.StockHistoryResponse;
import com.example.inventory.stock.out.StockHistoryRepository;
import com.example.inventory.stock.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockHistoryQueryUseCase {
    private final StockHistoryRepository stockHistoryRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Page<StockHistoryResponse> findHistories(Long productId, Pageable pageable) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(ProductNotFoundException::new);

        Pageable pageOnly = PageRequest
                .of(pageable.getPageNumber(), pageable.getPageSize());

        Page<StockHistory> histories = stockHistoryRepository
                .findByStockIdOrderByCreatedAtDescIdDesc(stock.getId(), pageOnly);

        return histories.map(StockHistoryResponse::from);
    }
}
