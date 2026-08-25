package com.ais.marketbackend.fel.domain.exception;

import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class EstadoDocumentoFelInvalidoException extends BusinessException {

    public EstadoDocumentoFelInvalidoException(EstadoDocumentoFel estadoActual) {
        super("La operación no es válida para un documento FEL en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_DOCUMENTO_FEL_INVALIDO";
    }
}
