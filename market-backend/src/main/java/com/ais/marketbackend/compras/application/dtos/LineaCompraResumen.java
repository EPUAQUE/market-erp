package com.ais.marketbackend.compras.application.dtos;

import java.math.BigDecimal;

public record LineaCompraResumen(Long id, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario) {
}
