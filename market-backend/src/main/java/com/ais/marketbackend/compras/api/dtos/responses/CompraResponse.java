package com.ais.marketbackend.compras.api.dtos.responses;

import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompraResponse {

    Long id;
    Long proveedorId;
    Long tiendaId;
    Instant fecha;
    EstadoCompra estado;
    List<LineaCompraResponse> lineas;
    String total;
}
