package com.ais.marketbackend.grupostienda.api.dtos.responses;

import com.ais.marketbackend.grupostienda.domain.model.EstadoGrupoTienda;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GrupoTiendaResponse {

    Long id;
    String codigo;
    String nombre;
    EstadoGrupoTienda estado;
}
