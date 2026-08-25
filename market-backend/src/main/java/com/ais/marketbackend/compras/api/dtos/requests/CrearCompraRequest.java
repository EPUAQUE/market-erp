package com.ais.marketbackend.compras.api.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CrearCompraRequest(
        @NotNull(message = "El proveedor es obligatorio") Long proveedorId,

        @NotEmpty(message = "La compra debe tener al menos una línea")
        List<@Valid LineaCompraRequest> lineas) {
}
