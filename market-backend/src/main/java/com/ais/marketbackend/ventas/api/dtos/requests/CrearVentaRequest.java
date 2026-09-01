package com.ais.marketbackend.ventas.api.dtos.requests;

import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
         * Fase 2 (PLAN_MEJORAS.md): obligatorio para TODO cliente HTTP (backoffice,
         * Flutter), no solo para sincronización offline — reintentar la misma
         * petición (timeout, doble clic, reintento de sincronización) nunca debe
         * crear una venta duplicada (ver {@code VentaService.crear}). Llamadas
         * directas al service layer (bypaseando el controller, ej. tests/seeders)
         * siguen aceptando {@code null} — esta validación solo aplica en la frontera
         * HTTP.
         */
        @NotBlank(message = "El correlationId es obligatorio")
        @Size(max = 100, message = "El correlationId no puede exceder 100 caracteres")
        String correlationId) {
}
