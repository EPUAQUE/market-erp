package com.ais.marketbackend.traslados.application.dtos;

import java.math.BigDecimal;

public record LineaTrasladoResumen(Long id, Long productoId, BigDecimal cantidad) {
}
