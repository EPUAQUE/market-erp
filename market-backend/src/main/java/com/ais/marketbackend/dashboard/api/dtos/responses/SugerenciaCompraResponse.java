package com.ais.marketbackend.dashboard.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SugerenciaCompraResponse {

    Long productoId;
    String existenciaActual;
    String stockMinimo;
    String cantidadSugerida;
}
