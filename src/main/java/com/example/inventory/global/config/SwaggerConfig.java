package com.example.inventory.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "재고 관리 API",
        version = "v1",
        description = """
                입고·출고·재고 조회 API. 동시 입출고는 재고 행 비관적 락으로 정합성을 지킨다.

                **에러 응답 형식** `{"code": "...", "message": "...", "timestamp": "..."}`

                - 400 `INVALID_REQUEST`: 입력 검증 실패, 깨진 JSON, 잘못된 경로 값
                - 404 `PRODUCT_NOT_FOUND`: 상품 없음
                - 409 `INSUFFICIENT_STOCK`: 재고 부족
                - 409 `IDEMPOTENCY_CONFLICT`: 같은 멱등 키로 다른 요청
                - 500 `INTERNAL_ERROR`: 서버 오류
                """))
public class SwaggerConfig {
}
