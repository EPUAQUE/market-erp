package com.ais.marketbackend.caja.api.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AbrirCajaRequest(
        @NotNull(message = "El monto inicial es obligatorio")
        @DecimalMin(value = "0", message = "El monto inicial no puede ser negativo")
        BigDecimal montoInicial) {
}
