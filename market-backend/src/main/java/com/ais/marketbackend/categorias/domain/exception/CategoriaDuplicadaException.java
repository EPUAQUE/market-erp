package com.ais.marketbackend.categorias.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CategoriaDuplicadaException extends BusinessException {

    public CategoriaDuplicadaException(String nombre) {
        super("Ya existe una categoría con el nombre '" + nombre + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "CATEGORIA_DUPLICADA";
    }
}
