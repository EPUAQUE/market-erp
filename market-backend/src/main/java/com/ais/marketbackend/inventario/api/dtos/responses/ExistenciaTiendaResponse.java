package com.ais.marketbackend.inventario.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExistenciaTiendaResponse {

    Long tiendaId;
    String tiendaNombre;
    String existenciaActual;
}
