package com.ais.marketbackend.reportes.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class RangoFechasInvalidoException extends BusinessException {

    public RangoFechasInvalidoException() {
        super("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "RANGO_FECHAS_INVALIDO";
    }
}
