package com.ais.marketbackend.dashboard.application.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Suma de {@link DashboardResumen} por cada tienda del grupo. Deliberadamente sin las
 * listas de acción (vencimientos, cobros/pagos pendientes, recordatorios, sugerencias):
 * son accionables por tienda, no tiene sentido fusionarlas sin perder a qué tienda
 * pertenece cada una — el drill-down por tienda individual sigue disponible para eso.
 * {@code cajaAbierta} (booleano) no aplica a un grupo: se reemplaza por el conteo de
 * cuántas tiendas del grupo tienen caja abierta.
 */
public record DashboardGrupoResumen(
        Long grupoId,
        List<Long> tiendaIds,

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

        // Caja (agregada: no hay un solo estado de caja para todo el grupo)
        long tiendasConCajaAbierta,
        long totalTiendas,
        BigDecimal cajaSaldoEsperadoTotal,
        BigDecimal ingresosHoy,
        BigDecimal egresosHoy,

        // Alertas
        long alertasCriticas,
        long alertasPreventivas) {
}
