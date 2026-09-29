package com.example.inventory.stock.domain;

/** 입출고 수량은 1 이상이어야 한다. 컨트롤러 검증을 거치지 않는 호출에서도 지켜지는 불변식이다 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(int amount) {
        super("수량은 1 이상이어야 합니다. 수량: " + amount);
    }
}
