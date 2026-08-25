package com.ais.marketbackend.gastosprogramados.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class GastoInactivoException extends BusinessException {

    public GastoInactivoException() {
        super("No se puede generar un pago para un gasto programado inactivo.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "GASTO_INACTIVO";
    }
}
