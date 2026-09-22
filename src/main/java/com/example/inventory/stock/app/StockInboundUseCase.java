package com.example.inventory.stock.app;

import com.example.inventory.product.app.ProductRegisterUseCase;
import com.example.inventory.product.domain.Product;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.domain.StockHistory;
import com.example.inventory.stock.domain.StockHistoryType;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.stock.out.StockHistoryRepository;
import com.example.inventory.stock.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockInboundUseCase {
    private final StockHistoryRepository stockHistoryRepository;
    private final StockRepository stockRepository;
    private final ProductRegisterUseCase productRegisterUseCase;

    @Transactional
    public StockResponse inbound(String sku, String name, int quantity) {
        Product product = productRegisterUseCase.registerIfAbsent(sku, name);
        Long productId = product.getId();
        stockRepository.insertIfAbsent(productId);

        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("재고 행 없음: productId= " + productId));

        stock.increase(quantity);
        StockHistory stockHistory = new StockHistory(stock, StockHistoryType.INBOUND, quantity);
        stockHistoryRepository.save(stockHistory);

        return StockResponse.from(stock);
    }

}
