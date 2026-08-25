package com.ais.marketbackend.marcas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class MarcaDuplicadaException extends BusinessException {

    public MarcaDuplicadaException(String nombre) {
        super("Ya existe una marca con el nombre '" + nombre + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "MARCA_DUPLICADA";
    }
}
