package com.example.inventory.stock.in;

import com.example.inventory.stock.app.StockHistoryQueryUseCase;
import com.example.inventory.stock.app.StockInboundUseCase;
import com.example.inventory.stock.app.StockOutboundUseCase;
import com.example.inventory.stock.app.StockQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "재고", description = "입고·출고·재고 조회·입출고 이력")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {
    private static final String IDEMPOTENCY_KEY_DESCRIPTION = "선택. 재시도해도 한 번만 반영하려면 요청마다 고유한 값을 넣는다. "
            + "같은 값으로 다시 보내면 처음 응답을 돌려주고, 같은 값에 다른 요청이면 409 IDEMPOTENCY_CONFLICT";

    private final StockQueryUseCase stockQueryUseCase;
    private final StockInboundUseCase stockInboundUseCase;
    private final StockOutboundUseCase stockOutboundUseCase;
    private final StockHistoryQueryUseCase stockHistoryQueryUseCase;

    @Operation(summary = "재고 조회", description = "상품 id로 현재 재고를 조회한다. 상품이 없으면 404 PRODUCT_NOT_FOUND")
    @GetMapping("/products/{productId}/stock")
    public StockResponse getStock(@PathVariable Long productId) {
        return stockQueryUseCase.findByProductId(productId);
    }

    @Operation(summary = "재고 현황 목록", description = "sku 오름차순, 한 페이지 20개 고정. page는 0부터 시작한다")
    @GetMapping("/stocks")
    public Page<StockResponse> getStocks(@RequestParam(defaultValue = "0") int page) {
        return stockQueryUseCase.findStocks(page);
    }

    @Operation(summary = "입고", description = "sku 기준으로 수량을 늘린다. 처음 보는 sku면 상품을 함께 등록하며 이때 name은 필수다(없으면 400). "
            + "기존 상품이면 name은 무시한다")
    @PostMapping("/stocks/inbound")
    public StockResponse inbound(
            @Valid @RequestBody InboundRequest request,
            @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return stockInboundUseCase.inbound(
                request.getSku(),
                request.getName(),
                request.getQuantity(),
                getIdempotencyKey(idempotencyKey)
        );
    }

    @Operation(summary = "출고", description = "재고보다 많으면 409 INSUFFICIENT_STOCK, 없는 sku면 404 PRODUCT_NOT_FOUND. "
            + "동시 요청은 재고 행 비관적 락으로 순서대로 처리한다")
    @PostMapping("/stocks/outbound")
    public StockResponse outbound(
            @Valid @RequestBody OutboundRequest request,
            @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return stockOutboundUseCase.outbound(
                request.getSku(),
                request.getQuantity(),
                getIdempotencyKey(idempotencyKey)
        );
    }

    @Operation(summary = "입출고 이력", description = "최신순 고정(sort 파라미터는 무시), size 최대 100. 상품이 없으면 404 PRODUCT_NOT_FOUND")
    @GetMapping("/products/{productId}/stock/histories")
    public Page<StockHistoryResponse> getStockHistories(@PathVariable Long productId, @ParameterObject Pageable pageable) {
        return stockHistoryQueryUseCase.findHistories(productId, pageable);
    }

    private String getIdempotencyKey(String idempotencyKey) {
        return StringUtils.hasText(idempotencyKey) ? idempotencyKey : null;
    }
}
