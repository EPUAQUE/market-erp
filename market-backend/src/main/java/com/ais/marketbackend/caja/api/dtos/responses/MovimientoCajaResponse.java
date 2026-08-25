package com.ais.marketbackend.caja.api.dtos.responses;

import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MovimientoCajaResponse {

    Long id;
    Instant fecha;
    TipoMovimientoCaja tipo;
    String concepto;
    String monto;
}
