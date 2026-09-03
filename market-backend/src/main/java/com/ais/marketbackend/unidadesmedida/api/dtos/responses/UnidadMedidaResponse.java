package com.ais.marketbackend.unidadesmedida.api.dtos.responses;

import com.ais.marketbackend.unidadesmedida.domain.model.EstadoUnidadMedida;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnidadMedidaResponse {

    Long id;
    String nombre;
    String abreviacion;
    EstadoUnidadMedida estado;
}
