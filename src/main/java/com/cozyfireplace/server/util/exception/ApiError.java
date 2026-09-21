package com.cozyfireplace.server.util.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Builder
public record ApiError(int status, String error, String message, Instant timestamp,
                       Map<String, String> validationErrors) {

    // Статический метод базовой ошибки
    public static ApiError of(HttpStatus status, String message) {
        return ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // Метод для ошибок с валидацией
    public static ApiError validationError(String message, Map<String, String> validationErrors) {
        return ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .timestamp(Instant.now())
                .validationErrors(validationErrors)
                .build();
    }
}
