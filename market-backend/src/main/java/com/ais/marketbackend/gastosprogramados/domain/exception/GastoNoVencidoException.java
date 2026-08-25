package com.ais.marketbackend.gastosprogramados.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class GastoNoVencidoException extends BusinessException {

    public GastoNoVencidoException() {
        super("El gasto programado aún no llega a su próxima fecha de vencimiento.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "GASTO_NO_VENCIDO";
    }
}
