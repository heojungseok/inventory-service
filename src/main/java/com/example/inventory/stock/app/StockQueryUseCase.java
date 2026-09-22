package com.example.inventory.stock.app;

import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryUseCase {
    private final ProductRepository productRepository;

    public void findByProductId(Long productId) {
        productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
    }

}
