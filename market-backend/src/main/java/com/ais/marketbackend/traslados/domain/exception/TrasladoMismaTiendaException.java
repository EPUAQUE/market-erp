package com.ais.marketbackend.traslados.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class TrasladoMismaTiendaException extends BusinessException {

    public TrasladoMismaTiendaException() {
        super("La tienda de origen y destino no pueden ser la misma.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "TRASLADO_MISMA_TIENDA";
    }
}
