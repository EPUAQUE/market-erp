package com.ais.marketbackend.seguridad.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class UsuarioDuplicadoException extends BusinessException {

    public UsuarioDuplicadoException(String username) {
        super("Ya existe un usuario con ese nombre de usuario.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "USUARIO_DUPLICADO";
    }
}
