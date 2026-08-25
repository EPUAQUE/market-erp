package com.ais.marketbackend.ventas.application.dtos;

import java.math.BigDecimal;

public record NuevaLineaVenta(Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
}
