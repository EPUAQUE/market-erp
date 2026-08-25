package com.ais.marketbackend.gastosprogramados.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PagoGastoResponse {

    Long id;
    Instant fecha;
    String monto;
}
