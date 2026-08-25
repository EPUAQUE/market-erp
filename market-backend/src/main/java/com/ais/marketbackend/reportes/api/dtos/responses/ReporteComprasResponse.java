package com.ais.marketbackend.reportes.api.dtos.responses;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReporteComprasResponse {

    Long tiendaId;
    Instant desde;
    Instant hasta;
    String totalCompras;
    long cantidadCompras;
    List<LineaReporteCompraResponse> lineas;
}
