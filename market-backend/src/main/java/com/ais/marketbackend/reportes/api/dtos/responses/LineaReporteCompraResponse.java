package com.ais.marketbackend.reportes.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineaReporteCompraResponse {

    Long compraId;
    Long proveedorId;
    Instant fecha;
    String total;
}
