package com.example.inventory.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "같은 멱등 키로 다른 요청이 들어왔습니다."),
    LOCK_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "다른 요청이 같은 재고를 처리하고 있습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

}
