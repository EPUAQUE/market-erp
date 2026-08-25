package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumen(
        Long tiendaId,

        // Ventas
        BigDecimal ventasHoyTotal,
        long ventasHoyCantidad,
        BigDecimal ventasMesTotal,
        long ventasMesCantidad,
        BigDecimal ventasMesAnteriorTotal,
        BigDecimal ticketPromedioMes,
        long facturasEmitidasMes,
        long facturasFelCertificadasMes,

        // Financiero
        BigDecimal utilidadMesTotal,
        BigDecimal margenPromedioMes,

        // Inventario
        BigDecimal inventarioValorizadoTotal,
        long productosAgotados,
        long productosBajoMinimo,
        long productosSinMovimiento,

        // Cuentas por cobrar (con aging)
        BigDecimal saldoPendienteCuentasPorCobrar,
        long cuentasPorCobrarVencidas,
        BigDecimal cxcAging0a30,
        BigDecimal cxcAging31a60,
        BigDecimal cxcAgingMas60,

        // Cuentas por pagar (con aging)
        BigDecimal saldoPendienteCuentasPorPagar,
        long cuentasPorPagarVencidas,
        BigDecimal cxpAging0a30,
        BigDecimal cxpAging31a60,
        BigDecimal cxpAgingMas60,

        // Caja
        boolean cajaAbierta,
        BigDecimal cajaSaldoEsperado,
        BigDecimal ingresosHoy,
        BigDecimal egresosHoy,

        // Alertas
        long alertasCriticas,
        long alertasPreventivas,

        // Widgets de acción
        List<VencimientoResumen> proximosVencimientos,
        List<CuentaPendienteResumen> topCobrosPendientes,
        List<CuentaPendienteResumen> topPagosPendientes,
        List<RecordatorioResumen> recordatorios,
        List<SugerenciaCompraResumen> sugerenciasCompra,
        List<SugerenciaTrasladoResumen> sugerenciasTraslado) {
}
