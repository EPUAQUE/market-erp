package com.ais.marketbackend.traslados.api.dtos.responses;

import com.ais.marketbackend.traslados.domain.model.EstadoTraslado;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrasladoResponse {

    Long id;
    Long tiendaOrigenId;
    Long tiendaDestinoId;
    Instant fecha;
    EstadoTraslado estado;
    List<LineaTrasladoResponse> lineas;
}
