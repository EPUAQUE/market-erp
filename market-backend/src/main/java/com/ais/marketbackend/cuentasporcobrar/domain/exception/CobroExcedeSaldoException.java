package com.ais.marketbackend.cuentasporcobrar.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CobroExcedeSaldoException extends BusinessException {

    public CobroExcedeSaldoException() {
        super("El monto del cobro no puede superar el saldo pendiente.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "COBRO_EXCEDE_SALDO";
    }
}
