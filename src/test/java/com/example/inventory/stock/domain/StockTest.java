package com.example.inventory.stock.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void 출고_하면_수량이_줄어든다() {
        Stock stock = new Stock(null, 10);

        stock.decrease(5);
        int after = stock.getQuantity();

        assertThat(after).isEqualTo(5);
    }

    @Test
    void 재고보다_많이_출고하면_예외() {
        Stock stock = new Stock(null, 10);

        assertThatThrownBy(() -> stock.decrease(11))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void 입고_하면_수량이_늘어난다() {
        Stock stock = new Stock(null, 10);
        stock.increase(7);

        int after = stock.getQuantity();

        assertThat(after).isEqualTo(17);
    }

    @Test
    void 음수_또는_0_수량은_입출고_거부() {
        Stock stock = new Stock(null, 3);

        assertThatThrownBy(() -> stock.increase(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.increase(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stock.decrease(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.decrease(0))
                .isInstanceOf(IllegalArgumentException.class);

    }

}
