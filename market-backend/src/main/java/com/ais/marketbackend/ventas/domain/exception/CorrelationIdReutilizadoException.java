package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un {@code correlationId} ya usado (dentro de la misma tienda y
 * vendedor) llega de nuevo con un cliente, líneas o método de pago distintos al
 * intento original — señal de que la clave de idempotencia se reutilizó para un
 * comando distinto, no de un reintento legítimo. Un reintento legítimo con el
 * mismo contenido nunca llega a lanzar esto: devuelve la venta existente tal cual.
 */
public class CorrelationIdReutilizadoException extends BusinessException {

    public CorrelationIdReutilizadoException(String correlationId) {
        super("El correlationId '" + correlationId
                + "' ya se usó para una venta distinta en esta tienda y vendedor.");
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
