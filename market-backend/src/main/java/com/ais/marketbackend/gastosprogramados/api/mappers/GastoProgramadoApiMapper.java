package com.ais.marketbackend.gastosprogramados.api.mappers;

import com.ais.marketbackend.gastosprogramados.api.dtos.responses.GastoProgramadoResponse;
import com.ais.marketbackend.gastosprogramados.api.dtos.responses.PagoGastoResponse;
import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.dtos.PagoGastoResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class GastoProgramadoApiMapper {

    public GastoProgramadoResponse toResponse(GastoProgramadoResumen resumen) {
        return GastoProgramadoResponse.builder()
                .id(resumen.id())
                .tiendaId(resumen.tiendaId())
                .concepto(resumen.concepto())
                .monto(toPlainString(resumen.monto()))
                .frecuencia(resumen.frecuencia())
                .proximaFecha(resumen.proximaFecha())
                .activo(resumen.activo())
                .pagos(resumen.pagos().stream().map(this::toResponse).toList())
                .build();
    }

    private PagoGastoResponse toResponse(PagoGastoResumen resumen) {
        return PagoGastoResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .monto(toPlainString(resumen.monto()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
