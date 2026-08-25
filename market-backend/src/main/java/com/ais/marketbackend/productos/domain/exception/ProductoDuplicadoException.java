package com.ais.marketbackend.productos.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ProductoDuplicadoException extends BusinessException {

    public ProductoDuplicadoException(String codigoInterno) {
        super("Ya existe un producto con el código interno '" + codigoInterno + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "PRODUCTO_DUPLICADO";
    }
}
