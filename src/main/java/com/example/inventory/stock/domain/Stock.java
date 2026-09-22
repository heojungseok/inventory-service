package com.example.inventory.stock.domain;

import com.example.inventory.product.domain.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "stock")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    private int quantity;
    @LastModifiedDate
    private Instant updatedAt;

    public Stock(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public void increase(int amount) {
        validateAmount(amount);
        quantity += amount;
    }

    public void decrease(int amount) {
        validateAmount(amount);

        if (amount > quantity) {
            throw new InsufficientStockException();
        }

        quantity -= amount;
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("잘못된 수량입니다. 수량: " + amount);
        }
    }

}
