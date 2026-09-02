package com.ais.marketbackend.inventario.api.dtos.requests;

import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RegistrarMovimientoRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 12, fraction = 0, message = "La cantidad debe ser un número entero")
        BigDecimal cantidad,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        @NotNull(message = "El tipo de movimiento es obligatorio") TipoMovimiento tipoMovimiento) {
}
