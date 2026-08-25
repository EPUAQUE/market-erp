package com.ais.marketbackend.dashboard.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CuentaPendienteResponse {

    Long id;
    Long contraparteId;
    String monto;
    Instant fechaVencimiento;
}
