package com.ais.marketbackend.traslados.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class TrasladoSinLineasException extends BusinessException {

    public TrasladoSinLineasException() {
        super("El traslado debe tener al menos una línea.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "TRASLADO_SIN_LINEAS";
    }
}
