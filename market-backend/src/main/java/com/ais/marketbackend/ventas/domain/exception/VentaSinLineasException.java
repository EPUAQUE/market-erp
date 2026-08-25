package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class VentaSinLineasException extends BusinessException {

    public VentaSinLineasException() {
        super("La venta debe tener al menos una línea.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "VENTA_SIN_LINEAS";
    }
}
