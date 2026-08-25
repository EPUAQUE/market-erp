package com.ais.marketbackend.caja.application.dtos;

import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoCajaResumen(Long id, Instant fecha, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
}
