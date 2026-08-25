package com.ais.marketbackend.traslados.api.mappers;

import com.ais.marketbackend.traslados.api.dtos.responses.LineaTrasladoResponse;
import com.ais.marketbackend.traslados.api.dtos.responses.TrasladoResponse;
import com.ais.marketbackend.traslados.application.dtos.LineaTrasladoResumen;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class TrasladoApiMapper {

    public TrasladoResponse toResponse(TrasladoResumen resumen) {
        return TrasladoResponse.builder()
                .id(resumen.id())
                .tiendaOrigenId(resumen.tiendaOrigenId())
                .tiendaDestinoId(resumen.tiendaDestinoId())
                .fecha(resumen.fecha())
                .estado(resumen.estado())
                .lineas(resumen.lineas().stream().map(this::toResponse).toList())
                .build();
    }

    private LineaTrasladoResponse toResponse(LineaTrasladoResumen resumen) {
        return LineaTrasladoResponse.builder()
                .id(resumen.id())
                .productoId(resumen.productoId())
                .cantidad(toPlainString(resumen.cantidad()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
