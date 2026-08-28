package com.ais.marketbackend.clientes.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ReferenciaInvalidaException extends BusinessException {

    public ReferenciaInvalidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "REFERENCIA_INVALIDA";
    }
}
