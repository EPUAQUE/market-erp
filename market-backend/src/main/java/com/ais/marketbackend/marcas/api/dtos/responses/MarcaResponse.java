package com.ais.marketbackend.marcas.api.dtos.responses;

import com.ais.marketbackend.marcas.domain.model.EstadoMarca;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarcaResponse {

    Long id;
    String nombre;
    EstadoMarca estado;
}
