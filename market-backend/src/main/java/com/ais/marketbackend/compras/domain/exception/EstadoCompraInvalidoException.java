package com.ais.marketbackend.compras.domain.exception;

import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class EstadoCompraInvalidoException extends BusinessException {

    public EstadoCompraInvalidoException(EstadoCompra estadoActual) {
        super("La operación no es válida para una compra en estado " + estadoActual + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ESTADO_COMPRA_INVALIDO";
    }
}
