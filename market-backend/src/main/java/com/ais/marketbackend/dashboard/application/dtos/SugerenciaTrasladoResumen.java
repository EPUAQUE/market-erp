package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;

public record SugerenciaTrasladoResumen(
        Long productoId, Long tiendaOrigenId, BigDecimal existenciaOrigen, BigDecimal cantidadSugerida) {
}
