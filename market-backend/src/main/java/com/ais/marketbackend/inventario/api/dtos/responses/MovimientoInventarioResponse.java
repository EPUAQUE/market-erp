package com.ais.marketbackend.inventario.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MovimientoInventarioResponse {

    Long id;
    Instant fecha;
    Long tiendaId;
    Long productoId;
    String cantidad;
    String costoUnitario;
    String tipoMovimiento;
}
