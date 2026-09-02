package com.ais.marketbackend.cuentasporcobrar.api.dtos.requests;

import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RegistrarCobroRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto no puede tener más de 2 decimales")
        BigDecimal monto,
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago) {
}
