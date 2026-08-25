package com.ais.marketbackend.ventas.api.dtos.requests;

import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearVentaRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,

        @NotEmpty(message = "La venta debe tener al menos una línea")
        List<@Valid LineaVentaRequest> lineas,

        @NotNull(message = "El método de pago es obligatorio") MetodoPago metodoPago,

        /**
         * Opcional — solo lo manda el cliente al sincronizar una venta creada
         * offline, para que un reintento de sincronización no cree una
         * venta duplicada (ver {@code VentaService.crear}).
         */
        @Size(max = 100, message = "El correlationId no puede exceder 100 caracteres")
        String correlationId) {
}
