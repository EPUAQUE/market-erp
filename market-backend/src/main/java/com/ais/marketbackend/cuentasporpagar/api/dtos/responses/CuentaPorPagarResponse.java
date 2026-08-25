package com.ais.marketbackend.cuentasporpagar.api.dtos.responses;

import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CuentaPorPagarResponse {

    Long id;
    Long compraId;
    Long proveedorId;
    Long tiendaId;
    Instant fechaEmision;
    Instant fechaVencimiento;
    String montoOriginal;
    String saldoPendiente;
    EstadoCuentaPorPagar estado;
    List<PagoResponse> pagos;
}
