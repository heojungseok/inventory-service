package com.example.inventory.product.app;

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
        // 신규 상품 규칙(상품명 필수)은 도메인이 검사한다
        Product candidate = new Product(sku, name);

        // 저장은 save() 대신 ON CONFLICT DO NOTHING으로 한다. 같은 sku가 동시에 들어오면 save()는 UNIQUE 위반으로
        // 트랜잭션 전체가 중단되지만, 이 방식은 늦은 요청을 조용히 넘겨 먼저 만들어진 상품에 이어서 입고하게 한다.
        productRepository.insertIfAbsent(candidate.getSku(), candidate.getName());

        return productRepository.findBySku(sku).orElseThrow(
                () -> new IllegalStateException("상품 등록 직후 조회 실패: " + sku)
        );
    }
}
