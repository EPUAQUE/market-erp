package com.ais.marketbackend.tiendas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class TiendaDuplicadaException extends BusinessException {

    public TiendaDuplicadaException(String codigo) {
        super("Ya existe una tienda con el código '" + codigo + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "TIENDA_DUPLICADA";
    }
}
