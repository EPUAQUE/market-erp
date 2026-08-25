package com.ais.marketbackend.caja.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CajaSesionAbiertaException extends BusinessException {

    public CajaSesionAbiertaException(Long tiendaId) {
        super("Ya hay una caja abierta para la tienda " + tiendaId + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "CAJA_SESION_ABIERTA";
    }
}
