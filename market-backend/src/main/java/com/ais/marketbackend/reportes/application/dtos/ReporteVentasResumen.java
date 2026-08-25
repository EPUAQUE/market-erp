package com.ais.marketbackend.reportes.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReporteVentasResumen(
        Long tiendaId, Instant desde, Instant hasta, BigDecimal totalVentas, long cantidadVentas,
        List<LineaReporteVenta> lineas) {
}
