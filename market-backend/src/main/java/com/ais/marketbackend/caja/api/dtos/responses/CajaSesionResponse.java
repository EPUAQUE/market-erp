package com.ais.marketbackend.caja.api.dtos.responses;

import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CajaSesionResponse {

    Long id;
    Long tiendaId;
    Instant fechaApertura;
    Instant fechaCierre;
    String montoInicial;
    String montoFinalContado;
    String saldoEsperado;
    EstadoCajaSesion estado;
    List<MovimientoCajaResponse> movimientos;
}
