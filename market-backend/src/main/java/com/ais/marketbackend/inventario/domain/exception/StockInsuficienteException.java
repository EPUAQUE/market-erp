package com.ais.marketbackend.inventario.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class StockInsuficienteException extends BusinessException {

    public StockInsuficienteException(Long productoId, Long tiendaId) {
        super("Existencia insuficiente del producto " + productoId + " en la tienda " + tiendaId + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "STOCK_INSUFICIENTE";
    }
}
