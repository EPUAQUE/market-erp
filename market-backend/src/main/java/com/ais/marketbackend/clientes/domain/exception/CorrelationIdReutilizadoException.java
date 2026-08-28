package com.ais.marketbackend.clientes.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un {@code correlationId} ya usado para dar de alta un cliente
 * llega de nuevo con datos distintos — señal de que la clave de idempotencia se
 * reutilizó para un alta distinta, no de un reintento legítimo. Un reintento
 * legítimo con el mismo contenido nunca llega a lanzar esto: devuelve el cliente
 * existente tal cual. Mismo patrón que
 * {@code ventas.domain.exception.CorrelationIdReutilizadoException}.
 */
public class CorrelationIdReutilizadoException extends BusinessException {

    public CorrelationIdReutilizadoException(String correlationId) {
        super("El correlationId '" + correlationId + "' ya se usó para dar de alta un cliente distinto.");
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
