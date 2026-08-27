package com.ais.marketbackend.dashboard.api.mappers;

import com.ais.marketbackend.dashboard.api.dtos.responses.CuentaPendienteResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.DashboardGrupoResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.DashboardResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.RecordatorioResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.SugerenciaCompraResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.SugerenciaTrasladoResponse;
import com.ais.marketbackend.dashboard.api.dtos.responses.VencimientoResponse;
import com.ais.marketbackend.dashboard.application.dtos.CuentaPendienteResumen;
import com.ais.marketbackend.dashboard.application.dtos.DashboardGrupoResumen;
import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;
import com.ais.marketbackend.dashboard.application.dtos.RecordatorioResumen;
import com.ais.marketbackend.dashboard.application.dtos.SugerenciaCompraResumen;
import com.ais.marketbackend.dashboard.application.dtos.SugerenciaTrasladoResumen;
import com.ais.marketbackend.dashboard.application.dtos.VencimientoResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con toPlainString(). */
@Component
public class DashboardApiMapper {

    public DashboardResponse toResponse(DashboardResumen resumen, boolean incluirFinanciero) {
        return DashboardResponse.builder()
                .tiendaId(resumen.tiendaId())
                .ventasHoyTotal(toPlainString(resumen.ventasHoyTotal()))
                .ventasHoyCantidad(resumen.ventasHoyCantidad())
                .ventasMesTotal(toPlainString(resumen.ventasMesTotal()))
                .ventasMesCantidad(resumen.ventasMesCantidad())
                .ventasMesAnteriorTotal(toPlainString(resumen.ventasMesAnteriorTotal()))
                .ticketPromedioMes(toPlainString(resumen.ticketPromedioMes()))
                .facturasEmitidasMes(resumen.facturasEmitidasMes())
                .facturasFelCertificadasMes(resumen.facturasFelCertificadasMes())
                .utilidadMesTotal(incluirFinanciero ? toPlainString(resumen.utilidadMesTotal()) : null)
                .margenPromedioMes(incluirFinanciero ? toPlainString(resumen.margenPromedioMes()) : null)
                .inventarioValorizadoTotal(toPlainString(resumen.inventarioValorizadoTotal()))
                .productosAgotados(resumen.productosAgotados())
                .productosBajoMinimo(resumen.productosBajoMinimo())
                .productosSinMovimiento(resumen.productosSinMovimiento())
                .saldoPendienteCuentasPorCobrar(toPlainString(resumen.saldoPendienteCuentasPorCobrar()))
                .cuentasPorCobrarVencidas(resumen.cuentasPorCobrarVencidas())
                .cxcAging0a30(toPlainString(resumen.cxcAging0a30()))
                .cxcAging31a60(toPlainString(resumen.cxcAging31a60()))
                .cxcAgingMas60(toPlainString(resumen.cxcAgingMas60()))
                .saldoPendienteCuentasPorPagar(toPlainString(resumen.saldoPendienteCuentasPorPagar()))
                .cuentasPorPagarVencidas(resumen.cuentasPorPagarVencidas())
                .cxpAging0a30(toPlainString(resumen.cxpAging0a30()))
                .cxpAging31a60(toPlainString(resumen.cxpAging31a60()))
                .cxpAgingMas60(toPlainString(resumen.cxpAgingMas60()))
                .cajaAbierta(resumen.cajaAbierta())
                .cajaSaldoEsperado(toPlainString(resumen.cajaSaldoEsperado()))
                .ingresosHoy(toPlainString(resumen.ingresosHoy()))
                .egresosHoy(toPlainString(resumen.egresosHoy()))
                .alertasCriticas(resumen.alertasCriticas())
                .alertasPreventivas(resumen.alertasPreventivas())
                .proximosVencimientos(resumen.proximosVencimientos().stream().map(this::toResponse).toList())
                .topCobrosPendientes(resumen.topCobrosPendientes().stream().map(this::toResponse).toList())
                .topPagosPendientes(resumen.topPagosPendientes().stream().map(this::toResponse).toList())
                .recordatorios(resumen.recordatorios().stream().map(this::toResponse).toList())
                .sugerenciasCompra(resumen.sugerenciasCompra().stream().map(this::toResponse).toList())
                .sugerenciasTraslado(resumen.sugerenciasTraslado().stream().map(this::toResponse).toList())
                .build();
    }

    public DashboardGrupoResponse toResponseGrupo(DashboardGrupoResumen resumen, boolean incluirFinanciero) {
        return DashboardGrupoResponse.builder()
                .grupoId(resumen.grupoId())
                .tiendaIds(resumen.tiendaIds())
                .ventasHoyTotal(toPlainString(resumen.ventasHoyTotal()))
                .ventasHoyCantidad(resumen.ventasHoyCantidad())
                .ventasMesTotal(toPlainString(resumen.ventasMesTotal()))
                .ventasMesCantidad(resumen.ventasMesCantidad())
                .ventasMesAnteriorTotal(toPlainString(resumen.ventasMesAnteriorTotal()))
                .ticketPromedioMes(toPlainString(resumen.ticketPromedioMes()))
                .facturasEmitidasMes(resumen.facturasEmitidasMes())
                .facturasFelCertificadasMes(resumen.facturasFelCertificadasMes())
                .utilidadMesTotal(incluirFinanciero ? toPlainString(resumen.utilidadMesTotal()) : null)
                .margenPromedioMes(incluirFinanciero ? toPlainString(resumen.margenPromedioMes()) : null)
                .inventarioValorizadoTotal(toPlainString(resumen.inventarioValorizadoTotal()))
                .productosAgotados(resumen.productosAgotados())
                .productosBajoMinimo(resumen.productosBajoMinimo())
                .productosSinMovimiento(resumen.productosSinMovimiento())
                .saldoPendienteCuentasPorCobrar(toPlainString(resumen.saldoPendienteCuentasPorCobrar()))
                .cuentasPorCobrarVencidas(resumen.cuentasPorCobrarVencidas())
                .cxcAging0a30(toPlainString(resumen.cxcAging0a30()))
                .cxcAging31a60(toPlainString(resumen.cxcAging31a60()))
                .cxcAgingMas60(toPlainString(resumen.cxcAgingMas60()))
                .saldoPendienteCuentasPorPagar(toPlainString(resumen.saldoPendienteCuentasPorPagar()))
                .cuentasPorPagarVencidas(resumen.cuentasPorPagarVencidas())
                .cxpAging0a30(toPlainString(resumen.cxpAging0a30()))
                .cxpAging31a60(toPlainString(resumen.cxpAging31a60()))
                .cxpAgingMas60(toPlainString(resumen.cxpAgingMas60()))
                .tiendasConCajaAbierta(resumen.tiendasConCajaAbierta())
                .totalTiendas(resumen.totalTiendas())
                .cajaSaldoEsperadoTotal(toPlainString(resumen.cajaSaldoEsperadoTotal()))
                .ingresosHoy(toPlainString(resumen.ingresosHoy()))
                .egresosHoy(toPlainString(resumen.egresosHoy()))
                .alertasCriticas(resumen.alertasCriticas())
                .alertasPreventivas(resumen.alertasPreventivas())
                .build();
    }

    private VencimientoResponse toResponse(VencimientoResumen v) {
        return VencimientoResponse.builder()
                .tipo(v.tipo())
                .referenciaId(v.referenciaId())
                .monto(toPlainString(v.monto()))
                .fechaVencimiento(v.fechaVencimiento())
                .build();
    }

    private CuentaPendienteResponse toResponse(CuentaPendienteResumen c) {
        return CuentaPendienteResponse.builder()
                .id(c.id())
                .contraparteId(c.contraparteId())
                .monto(toPlainString(c.monto()))
                .fechaVencimiento(c.fechaVencimiento())
                .build();
    }

    private RecordatorioResponse toResponse(RecordatorioResumen r) {
        return RecordatorioResponse.builder()
                .gastoProgramadoId(r.gastoProgramadoId())
                .concepto(r.concepto())
                .monto(toPlainString(r.monto()))
                .proximaFecha(r.proximaFecha())
                .build();
    }

    private SugerenciaCompraResponse toResponse(SugerenciaCompraResumen s) {
        return SugerenciaCompraResponse.builder()
                .productoId(s.productoId())
                .existenciaActual(toPlainString(s.existenciaActual()))
                .stockMinimo(toPlainString(s.stockMinimo()))
                .cantidadSugerida(toPlainString(s.cantidadSugerida()))
                .build();
    }

    private SugerenciaTrasladoResponse toResponse(SugerenciaTrasladoResumen s) {
        return SugerenciaTrasladoResponse.builder()
                .productoId(s.productoId())
                .tiendaOrigenId(s.tiendaOrigenId())
                .existenciaOrigen(toPlainString(s.existenciaOrigen()))
                .cantidadSugerida(toPlainString(s.cantidadSugerida()))
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
