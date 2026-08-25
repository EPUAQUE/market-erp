package com.ais.marketbackend.cuentasporcobrar.api.dtos.responses;

import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CuentaPorCobrarResponse {

    Long id;
    Long ventaId;
    Long clienteId;
    Long tiendaId;
    Instant fechaEmision;
    Instant fechaVencimiento;
    String montoOriginal;
    String saldoPendiente;
    EstadoCuentaPorCobrar estado;
    List<CobroResponse> cobros;
}
