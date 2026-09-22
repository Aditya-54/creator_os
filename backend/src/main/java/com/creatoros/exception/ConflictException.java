package com.creatoros.exception;

import org.springframework.http.HttpStatus;

/** Raised for state conflicts such as duplicate registration or a resource already locked/in-progress. */
public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
