package com.ais.marketbackend.dashboard.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecordatorioResponse {

    Long gastoProgramadoId;
    String concepto;
    String monto;
    Instant proximaFecha;
}
