package com.ais.marketbackend.caja.api.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull(message = "El monto contado es obligatorio")
        @DecimalMin(value = "0", message = "El monto contado no puede ser negativo")
        BigDecimal montoFinalContado) {
}
