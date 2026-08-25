package com.ais.marketbackend.inventario.application.dtos;

import java.math.BigDecimal;

public record InventarioResumen(
        Long id, Long tiendaId, Long productoId, BigDecimal existenciaActual, BigDecimal costoPromedioActual) {
}
