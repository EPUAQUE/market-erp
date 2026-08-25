package com.ais.marketbackend.cuentasporpagar.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PagoResponse {

    Long id;
    Instant fecha;
    String monto;
}
