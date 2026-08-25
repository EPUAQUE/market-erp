package com.ais.marketbackend.compras.api.mappers;

import com.ais.marketbackend.compras.api.dtos.responses.CompraResponse;
import com.ais.marketbackend.compras.api.dtos.responses.LineaCompraResponse;
import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.LineaCompraResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class CompraApiMapper {

    public CompraResponse toResponse(CompraResumen resumen) {
        return CompraResponse.builder()
                .id(resumen.id())
                .proveedorId(resumen.proveedorId())
                .tiendaId(resumen.tiendaId())
                .fecha(resumen.fecha())
                .estado(resumen.estado())
                .lineas(resumen.lineas().stream().map(this::toResponse).toList())
                .total(toPlainString(resumen.total()))
                .build();
    }

    private LineaCompraResponse toResponse(LineaCompraResumen resumen) {
        return LineaCompraResponse.builder()
                .id(resumen.id())
                .productoId(resumen.productoId())
                .cantidad(toPlainString(resumen.cantidad()))
                .costoUnitario(toPlainString(resumen.costoUnitario()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
