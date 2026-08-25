package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

/** {@code contraparteId}: clienteId (cobrar) o proveedorId (pagar) según el widget. */
public record CuentaPendienteResumen(Long id, Long contraparteId, BigDecimal monto, Instant fechaVencimiento) {
}
