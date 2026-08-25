package com.ais.marketbackend.cuentasporcobrar.api.dtos.responses;

import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CobroResponse {

    Long id;
    Instant fecha;
    String monto;
    MetodoPago metodoPago;
}
