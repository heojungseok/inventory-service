package com.example.inventory.product.app;

import com.example.inventory.global.exception.BusinessException;
import com.example.inventory.global.exception.ErrorCode;
import com.example.inventory.product.domain.Product;
import com.example.inventory.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductRegisterUseCase {
    private final ProductRepository productRepository;

    @Transactional
    public Product registerIfAbsent(String sku, String name) {
        Optional<Product> found = productRepository.findBySku(sku);

        if (found.isPresent()) {
            return found.get();
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        productRepository.insertIfAbsent(sku, name);

        return productRepository.findBySku(sku).orElseThrow(
                () -> new IllegalStateException("상품 등록 직후 조회 실패: " + sku)
        );
    }
}
