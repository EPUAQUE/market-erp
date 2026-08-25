package com.ais.marketbackend.ventas.api.dtos.responses;

import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VentaResponse {

    Long id;
    Long clienteId;
    Long tiendaId;
    Long vendedorId;
    Instant fecha;
    EstadoVenta estado;
    List<LineaVentaResponse> lineas;
    String total;
    MetodoPago metodoPago;
}
