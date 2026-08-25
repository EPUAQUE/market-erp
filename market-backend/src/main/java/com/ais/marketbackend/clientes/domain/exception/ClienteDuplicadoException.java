package com.ais.marketbackend.clientes.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ClienteDuplicadoException extends BusinessException {

    public ClienteDuplicadoException(String nit) {
        super("Ya existe un cliente con el NIT '" + nit + "'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "CLIENTE_DUPLICADO";
    }
}
