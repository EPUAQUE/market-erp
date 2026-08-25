package com.ais.marketbackend.ventas.application.dtos;

import java.math.BigDecimal;

public record LineaVentaResumen(Long id, Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
}
