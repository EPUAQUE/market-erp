package com.ais.marketbackend.reportes.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record LineaReporteVenta(Long ventaId, Long clienteId, Instant fecha, BigDecimal total) {
}
