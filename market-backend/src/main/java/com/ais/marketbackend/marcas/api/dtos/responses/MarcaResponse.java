package com.ais.marketbackend.marcas.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarcaResponse {

    Long id;
    String nombre;
}
