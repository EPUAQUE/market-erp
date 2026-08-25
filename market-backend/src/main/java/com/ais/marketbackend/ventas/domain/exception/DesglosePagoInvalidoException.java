package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Desglose de pagos inmediatos inválido al completar una venta {@code MIXTO}:
 * vacío, con un canal no admitido (solo EFECTIVO/TARJETA/TRANSFERENCIA — nunca
 * CREDITO/MIXTO como canal de un pago concreto), un monto no positivo, o cuya
 * suma excede el total de la venta.
 */
public class DesglosePagoInvalidoException extends BusinessException {

    public DesglosePagoInvalidoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "DESGLOSE_PAGO_INVALIDO";
    }
}
