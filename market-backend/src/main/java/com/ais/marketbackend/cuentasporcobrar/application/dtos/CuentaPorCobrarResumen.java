package com.ais.marketbackend.cuentasporcobrar.application.dtos;

import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CuentaPorCobrarResumen(
        Long id, Long ventaId, Long clienteId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
        BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorCobrar estado, List<CobroResumen> cobros) {
}
