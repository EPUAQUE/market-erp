package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import org.springframework.http.HttpStatus;

public class EstadoVentaInvalidoException extends BusinessException {

    public EstadoVentaInvalidoException(EstadoVenta estadoActual) {
        super("La operación no es válida para una venta en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_VENTA_INVALIDO";
    }
}
