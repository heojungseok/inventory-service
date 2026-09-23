package com.example.inventory.global.response;

import com.example.inventory.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class ErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.name(),
                message,
                Instant.now()
        );
    }
}
