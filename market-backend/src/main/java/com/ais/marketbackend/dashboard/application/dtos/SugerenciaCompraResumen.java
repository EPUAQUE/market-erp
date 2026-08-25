package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;

public record SugerenciaCompraResumen(
        Long productoId, BigDecimal existenciaActual, BigDecimal stockMinimo, BigDecimal cantidadSugerida) {
}
