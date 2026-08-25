package com.ais.marketbackend.cuentasporpagar.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class PagoExcedeSaldoException extends BusinessException {

    public PagoExcedeSaldoException() {
        super("El monto del pago no puede superar el saldo pendiente.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "PAGO_EXCEDE_SALDO";
    }
}
