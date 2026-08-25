package com.ais.marketbackend.ventas.api.dtos.requests;

import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PagoInmediatoRequest(
        @NotNull(message = "El método de pago es obligatorio") MetodoPago metodoPago,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        BigDecimal monto) {
}
