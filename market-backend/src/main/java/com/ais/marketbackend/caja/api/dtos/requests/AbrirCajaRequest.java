package com.ais.marketbackend.caja.api.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AbrirCajaRequest(
        @NotNull(message = "El monto inicial es obligatorio")
        @DecimalMin(value = "0", message = "El monto inicial no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto inicial no puede tener más de 2 decimales")
        BigDecimal montoInicial,

        /** Opcional — clave de idempotencia para reintentos seguros. */
        @Size(max = 100, message = "El correlationId no puede superar 100 caracteres")
        String correlationId) {
}
