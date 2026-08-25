package com.ais.marketbackend.ventas.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineaVentaResponse {

    Long id;
    Long productoId;
    String cantidad;
    String precioUnitario;
}
