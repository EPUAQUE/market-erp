package com.ais.marketbackend.productos.application.dtos;

import java.math.BigDecimal;

public record ProductoTiendaResumen(
        Long id, Long productoId, Long tiendaId, BigDecimal precioVenta, BigDecimal stockMinimo,
        BigDecimal stockMaximo, boolean permitirVenta, boolean permitirIngreso, boolean activo) {
}
