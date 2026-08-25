package com.ais.marketbackend.fel.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class VentaYaFacturadaException extends BusinessException {

    public VentaYaFacturadaException() {
        super("Esta venta ya tiene un documento FEL emitido.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "VENTA_YA_FACTURADA";
    }
}
