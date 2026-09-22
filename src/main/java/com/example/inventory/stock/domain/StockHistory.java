package com.example.inventory.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "stock_history")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    @Enumerated(EnumType.STRING)
    private StockHistoryType type;
    private int quantity;
    private int quantityAfter;
    @CreatedDate
    private Instant createdAt;

    public StockHistory(Stock stock, StockHistoryType type, int quantity) {
        this.stock = stock;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = stock.getQuantity();
    }
}
