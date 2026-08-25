package com.ais.marketbackend.reportes.api.dtos.responses;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReporteVentasResponse {

    Long tiendaId;
    Instant desde;
    Instant hasta;
    String totalVentas;
    long cantidadVentas;
    List<LineaReporteVentaResponse> lineas;
}
