package com.creatoros.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for domain/business rule violations that should be surfaced to
 * API clients as a 4xx response with a stable machine-readable {@code code}.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
