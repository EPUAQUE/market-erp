package com.ais.marketbackend.reportes.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record LineaReporteCompra(Long compraId, Long proveedorId, Instant fecha, BigDecimal total) {
}
