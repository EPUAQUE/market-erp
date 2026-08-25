package com.ais.marketbackend.caja.application.dtos;

import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CajaSesionResumen(
        Long id, Long tiendaId, Instant fechaApertura, Instant fechaCierre, BigDecimal montoInicial,
        BigDecimal montoFinalContado, BigDecimal saldoEsperado, EstadoCajaSesion estado,
        List<MovimientoCajaResumen> movimientos) {
}
