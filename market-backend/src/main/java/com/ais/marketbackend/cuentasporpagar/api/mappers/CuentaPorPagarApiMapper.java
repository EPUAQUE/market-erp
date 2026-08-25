package com.ais.marketbackend.cuentasporpagar.api.mappers;

import com.ais.marketbackend.cuentasporpagar.api.dtos.responses.CuentaPorPagarResponse;
import com.ais.marketbackend.cuentasporpagar.api.dtos.responses.PagoResponse;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.dtos.PagoResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class CuentaPorPagarApiMapper {

    public CuentaPorPagarResponse toResponse(CuentaPorPagarResumen resumen) {
        return CuentaPorPagarResponse.builder()
                .id(resumen.id())
                .compraId(resumen.compraId())
                .proveedorId(resumen.proveedorId())
                .tiendaId(resumen.tiendaId())
                .fechaEmision(resumen.fechaEmision())
                .fechaVencimiento(resumen.fechaVencimiento())
                .montoOriginal(toPlainString(resumen.montoOriginal()))
                .saldoPendiente(toPlainString(resumen.saldoPendiente()))
                .estado(resumen.estado())
                .pagos(resumen.pagos().stream().map(this::toResponse).toList())
                .build();
    }

    private PagoResponse toResponse(PagoResumen resumen) {
        return PagoResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .monto(toPlainString(resumen.monto()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
