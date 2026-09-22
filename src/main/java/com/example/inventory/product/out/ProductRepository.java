package com.example.inventory.product.out;

import com.example.inventory.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    @Modifying
    @Query(value = """
            INSERT INTO product (sku, name, created_at, updated_at)  
            VALUES (:sku, :name, now(), now()) 
            ON CONFLICT (sku) DO NOTHING 
            """, nativeQuery = true)
    int insertIfAbsent(@Param("sku") String sku, @Param("name") String name);
}
