package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordatorioResumen(Long gastoProgramadoId, String concepto, BigDecimal monto, Instant proximaFecha) {
}
