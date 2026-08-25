package com.ais.marketbackend.ventas.application.dtos;

import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VentaResumen(
        Long id, Long clienteId, Long tiendaId, Long vendedorId, Instant fecha, EstadoVenta estado,
        List<LineaVentaResumen> lineas, BigDecimal total, MetodoPago metodoPago) {
}
