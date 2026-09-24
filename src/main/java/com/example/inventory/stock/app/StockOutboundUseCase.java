package com.example.inventory.stock.app;

import com.example.inventory.product.domain.Product;
import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.product.out.ProductRepository;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.domain.StockHistory;
import com.example.inventory.stock.domain.StockHistoryType;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.stock.out.StockHistoryRepository;
import com.example.inventory.stock.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockOutboundUseCase {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final StockIdempotencyChecker stockIdempotencyChecker;

    @Transactional
    public StockResponse outbound(String sku, int quantity, String idempotencyKey) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(ProductNotFoundException::new);

        Long productId = product.getId();
        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("재고 행 없음: productId= " + productId));

        Optional<StockResponse> response =
                stockIdempotencyChecker.replay(idempotencyKey, stock, StockHistoryType.OUTBOUND, quantity);

        if (response.isPresent()) {
            return response.get();
        }

        stock.decrease(quantity);

        stockHistoryRepository.save(
                new StockHistory(
                        stock,
                        StockHistoryType.OUTBOUND,
                        quantity,
                        idempotencyKey
                )
        );

        return StockResponse.from(stock);
    }
}
