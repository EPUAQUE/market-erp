package com.ais.marketbackend.productos.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ConfiguracionTiendaDuplicadaException extends BusinessException {

    public ConfiguracionTiendaDuplicadaException(Long productoId, Long tiendaId) {
        super("El producto " + productoId + " ya tiene configuración en la tienda " + tiendaId + ".");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "PRODUCTO_TIENDA_DUPLICADO";
    }
}
