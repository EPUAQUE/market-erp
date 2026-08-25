package com.ais.marketbackend.fel.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CertificacionFallidaException extends BusinessException {

    public CertificacionFallidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }

    @Override
    public String errorCode() {
        return "CERTIFICACION_FALLIDA";
    }
}
