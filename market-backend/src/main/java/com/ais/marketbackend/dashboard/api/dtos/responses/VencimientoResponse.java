package com.ais.marketbackend.dashboard.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VencimientoResponse {

    String tipo;
    Long referenciaId;
    String monto;
    Instant fechaVencimiento;
}
