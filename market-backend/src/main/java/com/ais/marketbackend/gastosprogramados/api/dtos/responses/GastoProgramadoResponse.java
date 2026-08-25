package com.ais.marketbackend.gastosprogramados.api.dtos.responses;

import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GastoProgramadoResponse {

    Long id;
    Long tiendaId;
    String concepto;
    String monto;
    FrecuenciaGasto frecuencia;
    Instant proximaFecha;
    boolean activo;
    List<PagoGastoResponse> pagos;
}
