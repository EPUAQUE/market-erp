package com.ais.marketbackend.traslados.application.dtos;

import com.ais.marketbackend.traslados.domain.model.EstadoTraslado;
import java.time.Instant;
import java.util.List;

public record TrasladoResumen(
        Long id, Long tiendaOrigenId, Long tiendaDestinoId, Instant fecha, EstadoTraslado estado,
        List<LineaTrasladoResumen> lineas) {
}
