package com.ais.marketbackend.caja.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un {@code correlationId} ya usado (de apertura, movimiento o
 * cierre) llega de nuevo con datos distintos al intento original — señal de que
 * la clave de idempotencia se reutilizó para un comando distinto, no de un
 * reintento legítimo. Un reintento legítimo con el mismo contenido nunca llega a
 * lanzar esto: devuelve el estado existente tal cual. Ver
 * {@code ventas.domain.exception.CorrelationIdReutilizadoException}, mismo patrón.
 */
public class CorrelationIdReutilizadoException extends BusinessException {

    public CorrelationIdReutilizadoException(String correlationId) {
        super("El correlationId '" + correlationId + "' ya se usó para una operación de caja distinta.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "CORRELATION_ID_REUTILIZADO";
    }
}
