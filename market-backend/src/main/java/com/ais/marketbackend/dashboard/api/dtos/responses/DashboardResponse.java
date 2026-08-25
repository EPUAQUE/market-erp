package com.ais.marketbackend.dashboard.api.dtos.responses;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardResponse {

    Long tiendaId;

    String ventasHoyTotal;
    long ventasHoyCantidad;
    String ventasMesTotal;
    long ventasMesCantidad;
    String ventasMesAnteriorTotal;
    String ticketPromedioMes;
    long facturasEmitidasMes;
    long facturasFelCertificadasMes;

    String utilidadMesTotal;
    String margenPromedioMes;

    String inventarioValorizadoTotal;
    long productosAgotados;
    long productosBajoMinimo;
    long productosSinMovimiento;

    String saldoPendienteCuentasPorCobrar;
    long cuentasPorCobrarVencidas;
    String cxcAging0a30;
    String cxcAging31a60;
    String cxcAgingMas60;

    String saldoPendienteCuentasPorPagar;
    long cuentasPorPagarVencidas;
    String cxpAging0a30;
    String cxpAging31a60;
    String cxpAgingMas60;

    boolean cajaAbierta;
    String cajaSaldoEsperado;
    String ingresosHoy;
    String egresosHoy;

    long alertasCriticas;
    long alertasPreventivas;

    List<VencimientoResponse> proximosVencimientos;
    List<CuentaPendienteResponse> topCobrosPendientes;
    List<CuentaPendienteResponse> topPagosPendientes;
    List<RecordatorioResponse> recordatorios;
    List<SugerenciaCompraResponse> sugerenciasCompra;
    List<SugerenciaTrasladoResponse> sugerenciasTraslado;
}
