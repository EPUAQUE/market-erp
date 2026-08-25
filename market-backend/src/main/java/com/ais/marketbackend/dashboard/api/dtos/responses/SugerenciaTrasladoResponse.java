package com.ais.marketbackend.dashboard.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SugerenciaTrasladoResponse {

    Long productoId;
    Long tiendaOrigenId;
    String existenciaOrigen;
    String cantidadSugerida;
}
