package com.ais.marketbackend.productos.api.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AsignarProductoTiendaRequest(
        @NotNull(message = "La tienda es obligatoria") Long tiendaId,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0", message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta,

        @NotNull(message = "El stock mínimo es obligatorio")
        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,

        @NotNull(message = "El stock máximo es obligatorio")
        @DecimalMin(value = "0", message = "El stock máximo no puede ser negativo")
        BigDecimal stockMaximo,

        boolean permitirVenta,
        boolean permitirIngreso) {
}
