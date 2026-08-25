package com.ais.marketbackend.cuentasporcobrar.domain.exception;

import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class EstadoCuentaPorCobrarInvalidoException extends BusinessException {

    public EstadoCuentaPorCobrarInvalidoException(EstadoCuentaPorCobrar estadoActual) {
        super("La operación no es válida para una cuenta por cobrar en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_CUENTA_POR_COBRAR_INVALIDO";
    }
}
