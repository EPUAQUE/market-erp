package com.ais.marketbackend.unidadesmedida.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class UnidadMedidaDuplicadaException extends BusinessException {

    public UnidadMedidaDuplicadaException(String nombre) {
        super("Ya existe una unidad de medida con el nombre '" + nombre + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "UNIDAD_MEDIDA_DUPLICADA";
    }
}
