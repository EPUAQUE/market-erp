package com.ais.marketbackend.caja.api.dtos.requests;

import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarMovimientoCajaRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio") TipoMovimientoCaja tipo,

        @NotBlank(message = "El concepto es obligatorio") String concepto,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        /** Opcional — clave de idempotencia para reintentos seguros. */
        @Size(max = 100, message = "El correlationId no puede superar 100 caracteres")
        String correlationId) {
}
