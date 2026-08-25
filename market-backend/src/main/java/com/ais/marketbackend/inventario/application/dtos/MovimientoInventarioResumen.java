package com.ais.marketbackend.inventario.application.dtos;

import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoInventarioResumen(
        Long id, Instant fecha, Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
        TipoMovimiento tipoMovimiento) {
}
