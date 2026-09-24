package com.example.inventory.stock.out;

import com.example.inventory.stock.domain.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    Page<StockHistory> findByStockIdOrderByCreatedAtDescIdDesc(Long stockId, Pageable pageable);

    Optional<StockHistory> findByIdempotencyKey(String idempotencyKey);
}
