package com.ais.marketbackend.reportes.api.mappers;

import com.ais.marketbackend.reportes.api.dtos.responses.LineaReporteCompraResponse;
import com.ais.marketbackend.reportes.api.dtos.responses.LineaReporteVentaResponse;
import com.ais.marketbackend.reportes.api.dtos.responses.ReporteComprasResponse;
import com.ais.marketbackend.reportes.api.dtos.responses.ReporteVentasResponse;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteCompra;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteVenta;
import com.ais.marketbackend.reportes.application.dtos.ReporteComprasResumen;
import com.ais.marketbackend.reportes.application.dtos.ReporteVentasResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class ReporteApiMapper {

    public ReporteVentasResponse toResponse(ReporteVentasResumen resumen) {
        return ReporteVentasResponse.builder()
                .tiendaId(resumen.tiendaId())
                .desde(resumen.desde())
                .hasta(resumen.hasta())
                .totalVentas(toPlainString(resumen.totalVentas()))
                .cantidadVentas(resumen.cantidadVentas())
                .lineas(resumen.lineas().stream().map(this::toResponse).toList())
                .build();
    }

    public ReporteComprasResponse toResponse(ReporteComprasResumen resumen) {
        return ReporteComprasResponse.builder()
                .tiendaId(resumen.tiendaId())
                .desde(resumen.desde())
                .hasta(resumen.hasta())
                .totalCompras(toPlainString(resumen.totalCompras()))
                .cantidadCompras(resumen.cantidadCompras())
                .lineas(resumen.lineas().stream().map(this::toResponse).toList())
                .build();
    }

    private LineaReporteVentaResponse toResponse(LineaReporteVenta linea) {
        return LineaReporteVentaResponse.builder()
                .ventaId(linea.ventaId())
                .clienteId(linea.clienteId())
                .fecha(linea.fecha())
                .total(toPlainString(linea.total()))
                .build();
    }

    private LineaReporteCompraResponse toResponse(LineaReporteCompra linea) {
        return LineaReporteCompraResponse.builder()
                .compraId(linea.compraId())
                .proveedorId(linea.proveedorId())
                .fecha(linea.fecha())
                .total(toPlainString(linea.total()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
