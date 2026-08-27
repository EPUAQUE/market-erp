package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza al completar una venta que mueve efectivo/tarjeta/transferencia
 * (cualquier método salvo CREDITO) si la tienda no tiene una caja abierta —
 * sin esto, {@code CajaService.registrarMovimientoSiHayAbierta} descartaba el
 * ingreso en silencio (diseñado así a propósito para Cuentas por
 * Cobrar/Pagar, donde una tienda puede no operar caja diaria) y la venta se
 * completaba sin ningún registro de cuánto efectivo debería haber en caja.
 */
public class CajaNoAbiertaException extends BusinessException {

    public CajaNoAbiertaException(Long tiendaId) {
        super("No hay una caja abierta para la tienda " + tiendaId
                + ". Debe abrir turno antes de completar esta venta.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "CAJA_NO_ABIERTA";
    }
}
