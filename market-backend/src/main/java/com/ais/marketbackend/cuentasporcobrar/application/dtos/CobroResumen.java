package com.ais.marketbackend.cuentasporcobrar.application.dtos;

import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;

public record CobroResumen(Long id, Instant fecha, BigDecimal monto, MetodoPago metodoPago) {
}
