package com.ais.marketbackend.caja.api.mappers;

import com.ais.marketbackend.caja.api.dtos.responses.CajaSesionResponse;
import com.ais.marketbackend.caja.api.dtos.responses.MovimientoCajaResponse;
import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.dtos.MovimientoCajaResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class CajaApiMapper {

    public CajaSesionResponse toResponse(CajaSesionResumen resumen) {
        return CajaSesionResponse.builder()
                .id(resumen.id())
                .tiendaId(resumen.tiendaId())
                .fechaApertura(resumen.fechaApertura())
                .fechaCierre(resumen.fechaCierre())
                .montoInicial(toPlainString(resumen.montoInicial()))
                .montoFinalContado(toPlainString(resumen.montoFinalContado()))
                .saldoEsperado(toPlainString(resumen.saldoEsperado()))
                .estado(resumen.estado())
                .movimientos(resumen.movimientos().stream().map(this::toResponse).toList())
                .build();
    }

    private MovimientoCajaResponse toResponse(MovimientoCajaResumen resumen) {
        return MovimientoCajaResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .tipo(resumen.tipo())
                .concepto(resumen.concepto())
                .monto(toPlainString(resumen.monto()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
