package com.ais.marketbackend.cuentasporpagar.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoResumen(Long id, Instant fecha, BigDecimal monto) {
}
