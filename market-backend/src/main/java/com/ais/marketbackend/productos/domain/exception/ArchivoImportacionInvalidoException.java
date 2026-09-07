package com.ais.marketbackend.productos.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/** Se lanza cuando el archivo de carga masiva no es un .xlsx válido o no trae ninguna fila útil. */
public class ArchivoImportacionInvalidoException extends BusinessException {

    public ArchivoImportacionInvalidoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "ARCHIVO_IMPORTACION_INVALIDO";
    }
}
