package com.example.inventory.global.exception;

import com.example.inventory.global.response.ErrorResponse;
import com.example.inventory.product.domain.ProductNameRequiredException;
import com.example.inventory.product.domain.ProductNotFoundException;
import com.example.inventory.stock.app.IdempotencyConflictException;
import com.example.inventory.stock.domain.InsufficientStockException;
import com.example.inventory.stock.domain.InvalidQuantityException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /** V2에서 idempotency_key 컬럼에 UNIQUE를 걸 때 PostgreSQL이 붙인 이름 */
    private static final String IDEMPOTENCY_KEY_CONSTRAINT = "stock_history_idempotency_key_key";

    // 도메인·유스케이스 예외는 HTTP를 모른다. 어떤 에러 코드와 상태로 응답할지는 여기서만 정한다.

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound() {
        return error(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock() {
        return error(ErrorCode.INSUFFICIENT_STOCK);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict() {
        return error(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @ExceptionHandler({ProductNameRequiredException.class, InvalidQuantityException.class})
    public ResponseEntity<ErrorResponse> handleInvalidDomainValue(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest() {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLockTimeout() {
        return error(ErrorCode.LOCK_TIMEOUT);
    }

    /**
     * 서로 다른 상품에 같은 멱등 키가 동시에 쓰이면, 둘 다 키 조회를 통과한 뒤 늦은 쪽의 INSERT가 UNIQUE에 걸린다.
     * 이 경우만 409로 바꾸고 다른 제약 위반은 서버 오류로 남긴다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) throws Exception {
        if (isIdempotencyKeyViolation(e)) {
            return error(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return handleUnexpected(e);
    }

    private boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
        if (!(e.getCause() instanceof ConstraintViolationException)) {
            return false;
        }
        ConstraintViolationException violation = (ConstraintViolationException) e.getCause();
        return IDEMPOTENCY_KEY_CONSTRAINT.equals(violation.getConstraintName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) throws Exception {
        if (e instanceof org.springframework.web.ErrorResponse) {
            throw e; // 404·405처럼 스프링이 상태 코드를 아는 예외는 원래 처리로 돌려보낸다
        }

        log.error("처리하지 못한 예외 ", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ErrorResponse> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
