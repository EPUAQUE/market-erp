package com.ais.marketbackend.gastosprogramados.api.dtos.requests;

import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ActualizarGastoProgramadoRequest(
        @NotBlank(message = "El concepto es obligatorio")
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto no puede tener más de 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La frecuencia es obligatoria")
        FrecuenciaGasto frecuencia) {
}
