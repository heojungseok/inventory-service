package com.example.inventory.stock.out;

import com.example.inventory.stock.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    @Query(value = "select s from Stock s join fetch s.product where s.product.id = :productId ")
    Optional<Stock> findByProductId(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s join fetch s.product where s.product.id = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") Long productId);

    @Query(value = "select s from Stock s join fetch s.product p order by p.sku",
            countQuery = "select count(s) from Stock s")
    Page<Stock> findAllWithProduct(Pageable pageable);

    @Modifying
    @Query(value = """
        insert into stock (product_id, quantity, updated_at)
        values (:productId, 0, now())
        on conflict (product_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("productId") Long productId);
}
