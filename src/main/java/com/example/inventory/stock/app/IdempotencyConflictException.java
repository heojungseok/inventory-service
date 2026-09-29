package com.example.inventory.stock.app;

/** 이미 쓰인 멱등 키로 내용이 다른 요청이 들어왔다 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("같은 멱등 키로 다른 요청이 들어왔습니다.");
    }
}
