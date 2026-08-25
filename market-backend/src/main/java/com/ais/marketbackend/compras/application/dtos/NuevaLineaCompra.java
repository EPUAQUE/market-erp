package com.ais.marketbackend.compras.application.dtos;

import java.math.BigDecimal;

public record NuevaLineaCompra(Long productoId, BigDecimal cantidad, BigDecimal costoUnitario) {
}
