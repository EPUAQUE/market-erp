package com.ais.marketbackend.traslados.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import com.ais.marketbackend.traslados.domain.model.EstadoTraslado;
import org.springframework.http.HttpStatus;

public class EstadoTrasladoInvalidoException extends BusinessException {

    public EstadoTrasladoInvalidoException(EstadoTraslado estadoActual) {
        super("La operación no es válida para un traslado en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_TRASLADO_INVALIDO";
    }
}
