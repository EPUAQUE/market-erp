package com.ais.marketbackend.productos.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearProductoRequest(
        @NotBlank(message = "El código interno es obligatorio")
        @Size(max = 40, message = "El código interno no puede superar 40 caracteres")
        String codigoInterno,

        @Size(max = 40, message = "El código de barras no puede superar 40 caracteres")
        String codigoBarras,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "La categoría es obligatoria") Long categoriaId,
        @NotNull(message = "La marca es obligatoria") Long marcaId,
        @NotNull(message = "La unidad de medida es obligatoria") Long unidadMedidaId,

        @Size(max = 500, message = "La imagen no puede superar 500 caracteres")
        String imagenUrl) {
}
