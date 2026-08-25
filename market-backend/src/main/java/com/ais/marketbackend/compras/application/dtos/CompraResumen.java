package com.ais.marketbackend.compras.application.dtos;

import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompraResumen(
        Long id, Long proveedorId, Long tiendaId, Instant fecha, EstadoCompra estado,
        List<LineaCompraResumen> lineas, BigDecimal total) {
}
