package com.ais.marketbackend.inventario.application.dtos;

import java.math.BigDecimal;

/** Existencia de un producto en una tienda del grupo — ver {@code InventarioServiceImpl.listarExistenciaPorGrupo}. */
public record ExistenciaTiendaResumen(Long tiendaId, String tiendaNombre, BigDecimal existenciaActual) {
}
