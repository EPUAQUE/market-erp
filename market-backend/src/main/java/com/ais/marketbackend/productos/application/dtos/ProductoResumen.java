package com.ais.marketbackend.productos.application.dtos;

public record ProductoResumen(
        Long id, String codigoInterno, String codigoBarras, String nombre, String descripcion,
        Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl, boolean activo) {
}
