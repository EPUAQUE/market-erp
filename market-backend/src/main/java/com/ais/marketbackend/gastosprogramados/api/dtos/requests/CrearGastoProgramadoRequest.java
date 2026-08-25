package com.ais.marketbackend.gastosprogramados.api.dtos.requests;

import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record CrearGastoProgramadoRequest(
        @NotBlank(message = "El concepto es obligatorio")
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @NotNull(message = "La frecuencia es obligatoria")
        FrecuenciaGasto frecuencia,

        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant fechaInicio) {
}
