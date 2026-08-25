package com.ais.marketbackend.seguridad.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import java.time.Duration;
import org.springframework.http.HttpStatus;

public class RateLimitExcedidoException extends BusinessException {

    private final Duration retryAfter;

    public RateLimitExcedidoException(Duration retryAfter) {
        super("Demasiados intentos. Intente de nuevo más tarde.");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    @Override
    public String errorCode() {
        return "RATE_LIMIT_EXCEEDED";
    }
}
