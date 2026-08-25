package com.ais.marketbackend.traslados.application.dtos;

import java.math.BigDecimal;

public record NuevaLineaTraslado(Long productoId, BigDecimal cantidad) {
}
