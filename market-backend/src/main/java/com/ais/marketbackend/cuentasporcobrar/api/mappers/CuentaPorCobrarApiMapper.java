package com.ais.marketbackend.cuentasporcobrar.api.mappers;

import com.ais.marketbackend.cuentasporcobrar.api.dtos.responses.CobroResponse;
import com.ais.marketbackend.cuentasporcobrar.api.dtos.responses.CuentaPorCobrarResponse;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CobroResumen;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class CuentaPorCobrarApiMapper {

    public CuentaPorCobrarResponse toResponse(CuentaPorCobrarResumen resumen) {
        return CuentaPorCobrarResponse.builder()
                .id(resumen.id())
                .ventaId(resumen.ventaId())
                .clienteId(resumen.clienteId())
                .tiendaId(resumen.tiendaId())
                .fechaEmision(resumen.fechaEmision())
                .fechaVencimiento(resumen.fechaVencimiento())
                .montoOriginal(toPlainString(resumen.montoOriginal()))
                .saldoPendiente(toPlainString(resumen.saldoPendiente()))
                .estado(resumen.estado())
                .cobros(resumen.cobros().stream().map(this::toResponse).toList())
                .build();
    }

    private CobroResponse toResponse(CobroResumen resumen) {
        return CobroResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .monto(toPlainString(resumen.monto()))
                .metodoPago(resumen.metodoPago())
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
