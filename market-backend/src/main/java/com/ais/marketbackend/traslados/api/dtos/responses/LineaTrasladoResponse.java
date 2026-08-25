package com.ais.marketbackend.traslados.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineaTrasladoResponse {

    Long id;
    Long productoId;
    String cantidad;
}
