package com.ais.marketbackend.inventario.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

/** {@code existenciaActual}/{@code costoPromedioActual} viajan como String — ver ProductoTiendaResponse. */
@Value
@Builder
public class InventarioResponse {

    Long id;
    Long tiendaId;
    Long productoId;
    String existenciaActual;
    String costoPromedioActual;
}
