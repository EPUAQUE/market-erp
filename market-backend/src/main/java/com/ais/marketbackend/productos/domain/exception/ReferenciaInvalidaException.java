package com.ais.marketbackend.productos.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando una referencia a otro módulo (categoría, marca, unidad de
 * medida, tienda) no existe. La validación real la hace la FK de PostgreSQL —
 * ver {@code ProductoRepositoryAdapter}/{@code ProductoTiendaRepositoryAdapter},
 * que traducen la violación a esta excepción en vez de dejar fugar el error SQL.
 */
public class ReferenciaInvalidaException extends BusinessException {

    public ReferenciaInvalidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "REFERENCIA_INVALIDA";
    }
}
