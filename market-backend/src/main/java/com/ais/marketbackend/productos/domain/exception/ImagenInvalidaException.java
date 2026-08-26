package com.ais.marketbackend.productos.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ImagenInvalidaException extends BusinessException {

    public ImagenInvalidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "IMAGEN_INVALIDA";
    }
}
