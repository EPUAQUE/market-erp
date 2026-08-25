package com.ais.marketbackend.cuentasporpagar.application.dtos;

import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CuentaPorPagarResumen(
        Long id, Long compraId, Long proveedorId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
        BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorPagar estado, List<PagoResumen> pagos) {
}
