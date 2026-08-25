import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_provider.dart';
import '../../dashboard/data/dashboard_resumen.dart';
import '../../dashboard/presentation/dashboard_widgets.dart';

class DashboardEncargadoScreen extends ConsumerWidget {
  const DashboardEncargadoScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final resumenAsync = ref.watch(dashboardResumenProvider(tiendaId));

    return Scaffold(
      backgroundColor: DashboardPalette.surface,
      appBar: AppBar(
        backgroundColor: DashboardPalette.brand,
        foregroundColor: Colors.white,
        title: const Text('Dashboard · Tienda'),
      ),
      body: resumenAsync.when(
        data: (resumen) => _Contenido(tiendaId: tiendaId, resumen: resumen),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) =>
            Center(child: Text('No se pudo cargar el resumen: $error')),
      ),
    );
  }
}

class _Contenido extends ConsumerWidget {
  const _Contenido({required this.tiendaId, required this.resumen});

  final int tiendaId;
  final DashboardResumen resumen;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return RefreshIndicator(
      onRefresh: () async => ref.invalidate(dashboardResumenProvider(tiendaId)),
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (resumen.alertasCriticas > 0 || resumen.alertasPreventivas > 0)
            DashboardAlertBanner(
              criticas: resumen.alertasCriticas,
              preventivas: resumen.alertasPreventivas,
            ),
          const DashboardSectionHeader(
            titulo: 'Ventas',
            icon: Icons.point_of_sale_rounded,
            color: DashboardPalette.violet,
          ),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Hoy',
                  valor: 'Q ${resumen.ventasHoyTotal}',
                  subtitulo: '${resumen.ventasHoyCantidad} venta(s)',
                  icon: Icons.today_rounded,
                  color: DashboardPalette.violet,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Ticket promedio',
                  valor: 'Q ${resumen.ticketPromedioMes}',
                  icon: Icons.receipt_long_rounded,
                  color: DashboardPalette.info,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          DashboardComparisonBars(
            titulo: 'Ventas del mes vs. mes anterior',
            etiquetaActual: 'Este mes (${resumen.ventasMesCantidad} venta(s))',
            valorActual: resumen.ventasMesTotal,
            etiquetaAnterior: 'Mes anterior',
            valorAnterior: resumen.ventasMesAnteriorTotal,
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Facturas FEL certificadas',
                  valor:
                      '${resumen.facturasFelCertificadasMes}/${resumen.facturasEmitidasMes}',
                  icon: Icons.fact_check_rounded,
                  color: DashboardPalette.accent,
                ),
              ),
              if (resumen.utilidadMesTotal != null) ...[
                const SizedBox(width: 12),
                Expanded(
                  child: DashboardStatCard(
                    titulo: 'Utilidad del mes',
                    valor: 'Q ${resumen.utilidadMesTotal}',
                    subtitulo: resumen.margenPromedioMes != null
                        ? 'Margen ${resumen.margenPromedioMes}%'
                        : null,
                    icon: Icons.trending_up_rounded,
                    color: DashboardPalette.primary,
                  ),
                ),
              ],
            ],
          ),
          const DashboardSectionHeader(
            titulo: 'Caja',
            icon: Icons.point_of_sale_outlined,
            color: DashboardPalette.coral,
          ),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Estado',
                  valor: resumen.cajaAbierta ? 'Abierta' : 'Cerrada',
                  icon: resumen.cajaAbierta
                      ? Icons.lock_open_rounded
                      : Icons.lock_outline_rounded,
                  color: resumen.cajaAbierta
                      ? DashboardPalette.primary
                      : DashboardPalette.inkMuted,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Saldo esperado',
                  valor: resumen.cajaSaldoEsperado != null
                      ? 'Q ${resumen.cajaSaldoEsperado}'
                      : '—',
                  icon: Icons.account_balance_wallet_rounded,
                  color: DashboardPalette.coral,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Ingresos hoy',
                  valor: '+Q ${resumen.ingresosHoy}',
                  icon: Icons.arrow_downward_rounded,
                  color: DashboardPalette.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Egresos hoy',
                  valor: '-Q ${resumen.egresosHoy}',
                  icon: Icons.arrow_upward_rounded,
                  color: DashboardPalette.danger,
                ),
              ),
            ],
          ),
          const DashboardSectionHeader(
            titulo: 'Inventario',
            icon: Icons.inventory_2_rounded,
            color: DashboardPalette.info,
          ),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Valorizado',
                  valor: 'Q ${resumen.inventarioValorizadoTotal}',
                  icon: Icons.warehouse_rounded,
                  color: DashboardPalette.info,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Agotados',
                  valor: '${resumen.productosAgotados}',
                  icon: Icons.remove_shopping_cart_rounded,
                  color: resumen.productosAgotados > 0
                      ? DashboardPalette.danger
                      : DashboardPalette.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Bajo mínimo',
                  valor: '${resumen.productosBajoMinimo}',
                  icon: Icons.trending_down_rounded,
                  color: resumen.productosBajoMinimo > 0
                      ? DashboardPalette.warning
                      : DashboardPalette.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DashboardStatCard(
                  titulo: 'Sin movimiento (60d)',
                  valor: '${resumen.productosSinMovimiento}',
                  icon: Icons.hourglass_bottom_rounded,
                  color: DashboardPalette.inkMuted,
                ),
              ),
            ],
          ),
          const DashboardSectionHeader(
            titulo: 'Cuentas por cobrar',
            icon: Icons.call_received_rounded,
            color: DashboardPalette.primary,
          ),
          DashboardStatCard(
            titulo: 'Saldo pendiente',
            valor: 'Q ${resumen.saldoPendienteCuentasPorCobrar}',
            subtitulo: '${resumen.cuentasPorCobrarVencidas} vencida(s)',
            icon: Icons.request_page_rounded,
            color: resumen.cuentasPorCobrarVencidas > 0
                ? DashboardPalette.danger
                : DashboardPalette.primary,
          ),
          const SizedBox(height: 12),
          DashboardAgingRing(
            a0a30: resumen.cxcAging0a30,
            a31a60: resumen.cxcAging31a60,
            aMas60: resumen.cxcAgingMas60,
          ),
          if (resumen.topCobrosPendientes.isNotEmpty) ...[
            const SizedBox(height: 14),
            Text(
              'Top cobros pendientes',
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: DashboardPalette.inkMuted),
            ),
            const SizedBox(height: 8),
            DashboardListCard(
              children: resumen.topCobrosPendientes
                  .map(
                    (c) => DashboardListRow(
                      icon: Icons.person_outline_rounded,
                      color: DashboardPalette.primary,
                      titulo: 'Cliente #${c.contraparteId}',
                      subtitulo:
                          'Vence ${c.fechaVencimiento.toLocal().toString().split(' ').first}',
                      trailing: 'Q ${c.monto}',
                    ),
                  )
                  .toList(),
            ),
          ],
          const DashboardSectionHeader(
            titulo: 'Cuentas por pagar',
            icon: Icons.call_made_rounded,
            color: DashboardPalette.danger,
          ),
          DashboardStatCard(
            titulo: 'Saldo pendiente',
            valor: 'Q ${resumen.saldoPendienteCuentasPorPagar}',
            subtitulo: '${resumen.cuentasPorPagarVencidas} vencida(s)',
            icon: Icons.payments_rounded,
            color: resumen.cuentasPorPagarVencidas > 0
                ? DashboardPalette.danger
                : DashboardPalette.primary,
          ),
          const SizedBox(height: 12),
          DashboardAgingRing(
            a0a30: resumen.cxpAging0a30,
            a31a60: resumen.cxpAging31a60,
            aMas60: resumen.cxpAgingMas60,
          ),
          if (resumen.topPagosPendientes.isNotEmpty) ...[
            const SizedBox(height: 14),
            Text(
              'Top pagos pendientes',
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: DashboardPalette.inkMuted),
            ),
            const SizedBox(height: 8),
            DashboardListCard(
              children: resumen.topPagosPendientes
                  .map(
                    (c) => DashboardListRow(
                      icon: Icons.local_shipping_outlined,
                      color: DashboardPalette.danger,
                      titulo: 'Proveedor #${c.contraparteId}',
                      subtitulo:
                          'Vence ${c.fechaVencimiento.toLocal().toString().split(' ').first}',
                      trailing: 'Q ${c.monto}',
                    ),
                  )
                  .toList(),
            ),
          ],
          if (resumen.sugerenciasCompra.isNotEmpty) ...[
            const DashboardSectionHeader(
              titulo: 'Sugerencias de compra',
              icon: Icons.shopping_cart_rounded,
              color: DashboardPalette.warning,
            ),
            DashboardListCard(
              children: resumen.sugerenciasCompra
                  .map(
                    (s) => DashboardListRow(
                      icon: Icons.shopping_cart_outlined,
                      color: DashboardPalette.warning,
                      titulo: 'Producto #${s.productoId}',
                      subtitulo:
                          'Existencia ${s.existenciaActual} · mínimo ${s.stockMinimo}',
                      trailing: '+${s.cantidadSugerida}',
                    ),
                  )
                  .toList(),
            ),
          ],
          if (resumen.sugerenciasTraslado.isNotEmpty) ...[
            const DashboardSectionHeader(
              titulo: 'Sugerencias de traslado',
              icon: Icons.swap_horiz_rounded,
              color: DashboardPalette.brand,
            ),
            DashboardListCard(
              children: resumen.sugerenciasTraslado
                  .map(
                    (s) => DashboardListRow(
                      icon: Icons.swap_horiz_rounded,
                      color: DashboardPalette.brand,
                      titulo: 'Producto #${s.productoId}',
                      subtitulo:
                          'Desde tienda #${s.tiendaOrigenId} (existencia ${s.existenciaOrigen})',
                      trailing: '+${s.cantidadSugerida}',
                    ),
                  )
                  .toList(),
            ),
          ],
          const SizedBox(height: 12),
        ],
      ),
    );
  }
}
