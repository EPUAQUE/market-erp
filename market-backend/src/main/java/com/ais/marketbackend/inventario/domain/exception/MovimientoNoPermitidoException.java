package com.ais.marketbackend.inventario.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando {@code ProductoTienda.permitirIngreso}/{@code permitirVenta} (o
 * {@code activo}) bloquea el movimiento solicitado — ver
 * {@code InventarioServiceImpl.registrarMovimiento}.
 */
public class MovimientoNoPermitidoException extends BusinessException {

    public MovimientoNoPermitidoException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "MOVIMIENTO_NO_PERMITIDO";
    }
}
