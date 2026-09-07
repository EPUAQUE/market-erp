package com.ais.marketbackend.inventario.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un ingreso (movimiento COMPRA) de un producto en una tienda del grupo — ver
 * {@code InventarioService.listarUltimosIngresosPorGrupo}. {@code compraId} es el
 * {@code origenId} del movimiento, usado en la capa API para resolver el nombre
 * del proveedor (igual que {@code MovimientoInventarioResumen}).
 */
public record IngresoTiendaResumen(
        Long tiendaId, String tiendaNombre, Instant fecha, BigDecimal cantidad, BigDecimal costoUnitario,
        Long compraId) {
}
