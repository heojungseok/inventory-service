package com.example.inventory.stock.app;

import com.example.inventory.global.exception.BusinessException;
import com.example.inventory.global.exception.ErrorCode;
import com.example.inventory.stock.domain.Stock;
import com.example.inventory.stock.domain.StockHistory;
import com.example.inventory.stock.domain.StockHistoryType;
import com.example.inventory.stock.in.StockResponse;
import com.example.inventory.stock.out.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockIdempotencyChecker {
    private final StockHistoryRepository stockHistoryRepository;

    /** stock 락을 잡은 뒤 호출. 이미 처리한 요청이면 처음 응답을 주고 그렇지 않다면 empty를 반환 */
    public Optional<StockResponse> replay(String key, Stock stock, StockHistoryType type, int quantity) {
        if (key == null){
            return Optional.empty();
        }

        Optional<StockHistory> found = stockHistoryRepository.findByIdempotencyKey(key);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        if (!found.get().isSameRequest(stock, type, quantity)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }

        return Optional.of(StockResponse.fromHistory(found.get()));
    }
}
