package com.ais.marketbackend.cuentasporpagar.domain.exception;

import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class EstadoCuentaPorPagarInvalidoException extends BusinessException {

    public EstadoCuentaPorPagarInvalidoException(EstadoCuentaPorPagar estadoActual) {
        super("La operación no es válida para una cuenta por pagar en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_CUENTA_POR_PAGAR_INVALIDO";
    }
}
