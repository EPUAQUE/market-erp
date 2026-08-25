package com.ais.marketbackend.gastosprogramados.application.dtos;

import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record GastoProgramadoResumen(
        Long id, Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia,
        Instant proximaFecha, boolean activo, List<PagoGastoResumen> pagos) {
}
