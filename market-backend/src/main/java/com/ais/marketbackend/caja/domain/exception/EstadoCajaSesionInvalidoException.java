package com.ais.marketbackend.caja.domain.exception;

import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class EstadoCajaSesionInvalidoException extends BusinessException {

    public EstadoCajaSesionInvalidoException(EstadoCajaSesion estadoActual) {
        super("La operación no es válida para una caja en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_CAJA_SESION_INVALIDO";
    }
}
