package com.ais.marketbackend.inventario.api.dtos.responses;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IngresoTiendaResponse {

    Long tiendaId;
    String tiendaNombre;
    Instant fecha;
    String cantidad;
    String costoUnitario;

    /** Solo no-null si la compra/proveedor de origen todavía existen. */
    String proveedorNombre;
}
