package com.ais.marketbackend.gastosprogramados.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoGastoResumen(Long id, Instant fecha, BigDecimal monto) {
}
