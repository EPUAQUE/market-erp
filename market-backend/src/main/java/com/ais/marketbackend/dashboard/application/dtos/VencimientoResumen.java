package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

/** {@code tipo}: {@code "CUENTA_POR_COBRAR"} o {@code "CUENTA_POR_PAGAR"}. */
public record VencimientoResumen(String tipo, Long referenciaId, BigDecimal monto, Instant fechaVencimiento) {
}
