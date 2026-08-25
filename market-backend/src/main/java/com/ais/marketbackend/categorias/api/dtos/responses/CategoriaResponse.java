package com.ais.marketbackend.categorias.api.dtos.responses;

import com.ais.marketbackend.categorias.domain.model.EstadoCategoria;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoriaResponse {

    Long id;
    String nombre;
    String imagen;
    EstadoCategoria estado;
}
