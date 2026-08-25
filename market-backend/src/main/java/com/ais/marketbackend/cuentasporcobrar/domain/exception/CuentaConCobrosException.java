package com.ais.marketbackend.cuentasporcobrar.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/** Se lanza al intentar anular una cuenta que ya tiene cobros registrados — anularla corrompería el historial. */
public class CuentaConCobrosException extends BusinessException {

    public CuentaConCobrosException() {
        super("No se puede anular una cuenta por cobrar con abonos ya registrados.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "CUENTA_CON_COBROS";
    }
}
