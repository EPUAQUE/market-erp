package com.ais.marketbackend.traslados.api.dtos.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LineaTrasladoRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 12, fraction = 0, message = "La cantidad debe ser un número entero")
        BigDecimal cantidad) {
}
