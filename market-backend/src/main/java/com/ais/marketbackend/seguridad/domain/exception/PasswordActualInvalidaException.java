package com.ais.marketbackend.seguridad.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class PasswordActualInvalidaException extends BusinessException {

    public PasswordActualInvalidaException() {
        super("La contraseña actual no es correcta.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "PASSWORD_ACTUAL_INVALIDA";
    }
}
