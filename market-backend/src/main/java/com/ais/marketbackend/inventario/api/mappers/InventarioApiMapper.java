package com.ais.marketbackend.inventario.api.mappers;

import com.ais.marketbackend.inventario.api.dtos.responses.InventarioResponse;
import com.ais.marketbackend.inventario.api.dtos.responses.MovimientoInventarioResponse;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class InventarioApiMapper {

    public InventarioResponse toResponse(InventarioResumen resumen) {
        return InventarioResponse.builder()
                .id(resumen.id())
                .tiendaId(resumen.tiendaId())
                .productoId(resumen.productoId())
                .existenciaActual(toPlainString(resumen.existenciaActual()))
                .costoPromedioActual(toPlainString(resumen.costoPromedioActual()))
                .build();
    }

    public MovimientoInventarioResponse toResponse(MovimientoInventarioResumen resumen) {
        return MovimientoInventarioResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .tiendaId(resumen.tiendaId())
                .productoId(resumen.productoId())
                .cantidad(toPlainString(resumen.cantidad()))
                .costoUnitario(toPlainString(resumen.costoUnitario()))
                .tipoMovimiento(resumen.tipoMovimiento().name())
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
