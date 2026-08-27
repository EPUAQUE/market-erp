package com.ais.marketbackend.grupostienda.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class GrupoTiendaDuplicadoException extends BusinessException {

    public GrupoTiendaDuplicadoException(String codigo) {
        super("Ya existe un grupo de tiendas con el código '" + codigo + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "GRUPO_TIENDA_DUPLICADO";
    }
}
