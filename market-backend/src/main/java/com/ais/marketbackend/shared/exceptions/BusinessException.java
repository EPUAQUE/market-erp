package com.ais.marketbackend.shared.exceptions;

import org.springframework.http.HttpStatus;

/** Base class for domain/business rule violations that map to a controlled HTTP response. */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    public abstract HttpStatus httpStatus();

    public abstract String errorCode();
}
