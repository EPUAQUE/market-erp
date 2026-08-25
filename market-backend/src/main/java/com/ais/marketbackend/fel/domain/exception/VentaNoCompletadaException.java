package com.ais.marketbackend.fel.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class VentaNoCompletadaException extends BusinessException {

    public VentaNoCompletadaException() {
        super("Solo se puede emitir FEL para una venta completada.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "VENTA_NO_COMPLETADA";
    }
}
