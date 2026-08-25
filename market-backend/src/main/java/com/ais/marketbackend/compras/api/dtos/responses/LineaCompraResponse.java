package com.ais.marketbackend.compras.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineaCompraResponse {

    Long id;
    Long productoId;
    String cantidad;
    String costoUnitario;
}
