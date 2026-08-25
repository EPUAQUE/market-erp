package com.ais.marketbackend.proveedores.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ProveedorDuplicadoException extends BusinessException {

    public ProveedorDuplicadoException(String nit) {
        super("Ya existe un proveedor con el NIT '" + nit + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "PROVEEDOR_DUPLICADO";
    }
}
