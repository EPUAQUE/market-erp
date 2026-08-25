package com.ais.marketbackend.compras.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CompraSinLineasException extends BusinessException {

    public CompraSinLineasException() {
        super("La compra debe tener al menos una línea.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "COMPRA_SIN_LINEAS";
    }
}
