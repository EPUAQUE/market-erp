package com.ais.marketbackend.reportes.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineaReporteVentaResponse {

    Long ventaId;
    Long clienteId;
    Instant fecha;
    String total;
}
