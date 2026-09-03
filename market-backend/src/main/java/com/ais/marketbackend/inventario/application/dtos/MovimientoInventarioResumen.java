package com.ais.marketbackend.inventario.application.dtos;

import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Instant;

/** {@code origenId}: id de la compra/venta/traslado que originó el movimiento — null para ajustes manuales o movimientos previos a este campo. */
public record MovimientoInventarioResumen(
        Long id, Instant fecha, Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
        TipoMovimiento tipoMovimiento, Long origenId) {
}
