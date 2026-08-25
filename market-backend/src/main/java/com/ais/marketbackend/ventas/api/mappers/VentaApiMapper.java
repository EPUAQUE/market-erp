package com.ais.marketbackend.ventas.api.mappers;

import com.ais.marketbackend.ventas.api.dtos.responses.LineaVentaResponse;
import com.ais.marketbackend.ventas.api.dtos.responses.VentaResponse;
import com.ais.marketbackend.ventas.application.dtos.LineaVentaResumen;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class VentaApiMapper {

    public VentaResponse toResponse(VentaResumen resumen) {
        return VentaResponse.builder()
                .id(resumen.id())
                .clienteId(resumen.clienteId())
                .tiendaId(resumen.tiendaId())
                .vendedorId(resumen.vendedorId())
                .fecha(resumen.fecha())
                .estado(resumen.estado())
                .lineas(resumen.lineas().stream().map(this::toResponse).toList())
                .total(toPlainString(resumen.total()))
                .metodoPago(resumen.metodoPago())
                .build();
    }

    private LineaVentaResponse toResponse(LineaVentaResumen resumen) {
        return LineaVentaResponse.builder()
                .id(resumen.id())
                .productoId(resumen.productoId())
                .cantidad(toPlainString(resumen.cantidad()))
                .precioUnitario(toPlainString(resumen.precioUnitario()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
