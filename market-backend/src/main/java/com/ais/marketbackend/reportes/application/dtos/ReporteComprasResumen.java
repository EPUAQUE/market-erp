package com.ais.marketbackend.reportes.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReporteComprasResumen(
        Long tiendaId, Instant desde, Instant hasta, BigDecimal totalCompras, long cantidadCompras,
        List<LineaReporteCompra> lineas) {
}
