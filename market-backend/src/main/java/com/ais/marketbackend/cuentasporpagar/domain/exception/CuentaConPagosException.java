package com.ais.marketbackend.cuentasporpagar.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/** Se lanza al intentar anular una cuenta que ya tiene abonos registrados — anularla corrompería el historial. */
public class CuentaConPagosException extends BusinessException {

    public CuentaConPagosException() {
        super("No se puede anular una cuenta por pagar con abonos ya registrados.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "CUENTA_CON_PAGOS";
    }
}
