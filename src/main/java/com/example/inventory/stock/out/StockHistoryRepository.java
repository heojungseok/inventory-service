package com.example.inventory.stock.out;

import com.example.inventory.stock.domain.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    Page<StockHistory> findByStockIdOrderByCreatedAtDescIdDesc(Long stockId, Pageable pageable);
}
