import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_provider.dart';
import '../../dashboard/data/dashboard_resumen.dart';

const _brand = Color(0xFF0F4C5C);
const _primary = Color(0xFF2E8B57);
const _danger = Color(0xFFDC6B6B);
const _warning = Color(0xFFF4B942);

class DashboardEncargadoScreen extends ConsumerWidget {
  const DashboardEncargadoScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final resumenAsync = ref.watch(dashboardResumenProvider(tiendaId));

    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      appBar: AppBar(
        backgroundColor: _brand,
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
            _AlertasBanner(resumen: resumen),
          const _SeccionTitulo('Ventas'),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Hoy',
                  valor: 'Q ${resumen.ventasHoyTotal}',
                  subtitulo: '${resumen.ventasHoyCantidad} venta(s)',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Este mes',
                  valor: 'Q ${resumen.ventasMesTotal}',
                  subtitulo: '${resumen.ventasMesCantidad} venta(s)',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Mes anterior',
                  valor: 'Q ${resumen.ventasMesAnteriorTotal}',
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Ticket promedio del mes',
                  valor: 'Q ${resumen.ticketPromedioMes}',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Facturas FEL certificadas',
                  valor:
                      '${resumen.facturasFelCertificadasMes}/${resumen.facturasEmitidasMes}',
                ),
              ),
              if (resumen.utilidadMesTotal != null) ...[
                const SizedBox(width: 12),
                Expanded(
                  child: _StatCard(
                    titulo: 'Utilidad del mes',
                    valor: 'Q ${resumen.utilidadMesTotal}',
                    subtitulo: resumen.margenPromedioMes != null
                        ? 'Margen ${resumen.margenPromedioMes}%'
                        : null,
                  ),
                ),
              ],
            ],
          ),
          const _SeccionTitulo('Caja'),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Estado',
                  valor: resumen.cajaAbierta ? 'Abierta' : 'Cerrada',
                  colorValor: resumen.cajaAbierta ? _primary : Colors.black54,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Saldo esperado',
                  valor: resumen.cajaSaldoEsperado != null
                      ? 'Q ${resumen.cajaSaldoEsperado}'
                      : '—',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Ingresos / egresos hoy',
                  valor: '+Q ${resumen.ingresosHoy} / -Q ${resumen.egresosHoy}',
                ),
              ),
            ],
          ),
          const _SeccionTitulo('Inventario'),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Valorizado',
                  valor: 'Q ${resumen.inventarioValorizadoTotal}',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Agotados',
                  valor: '${resumen.productosAgotados}',
                  colorValor: resumen.productosAgotados > 0
                      ? _danger
                      : _primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Bajo mínimo',
                  valor: '${resumen.productosBajoMinimo}',
                  colorValor: resumen.productosBajoMinimo > 0
                      ? _warning
                      : _primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Sin movimiento (60d)',
                  valor: '${resumen.productosSinMovimiento}',
                ),
              ),
            ],
          ),
          const _SeccionTitulo('Cuentas por cobrar'),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Saldo pendiente',
                  valor: 'Q ${resumen.saldoPendienteCuentasPorCobrar}',
                  subtitulo: '${resumen.cuentasPorCobrarVencidas} vencida(s)',
                  colorValor: resumen.cuentasPorCobrarVencidas > 0
                      ? _danger
                      : _primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _AgingCard(
                  a0a30: resumen.cxcAging0a30,
                  a31a60: resumen.cxcAging31a60,
                  aMas60: resumen.cxcAgingMas60,
                ),
              ),
            ],
          ),
          if (resumen.topCobrosPendientes.isNotEmpty)
            _ListaCuentasPendientes(
              titulo: 'Top cobros pendientes',
              cuentas: resumen.topCobrosPendientes,
              etiquetaContraparte: 'Cliente',
            ),
          const _SeccionTitulo('Cuentas por pagar'),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Saldo pendiente',
                  valor: 'Q ${resumen.saldoPendienteCuentasPorPagar}',
                  subtitulo: '${resumen.cuentasPorPagarVencidas} vencida(s)',
                  colorValor: resumen.cuentasPorPagarVencidas > 0
                      ? _danger
                      : _primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _AgingCard(
                  a0a30: resumen.cxpAging0a30,
                  a31a60: resumen.cxpAging31a60,
                  aMas60: resumen.cxpAgingMas60,
                ),
              ),
            ],
          ),
          if (resumen.topPagosPendientes.isNotEmpty)
            _ListaCuentasPendientes(
              titulo: 'Top pagos pendientes',
              cuentas: resumen.topPagosPendientes,
              etiquetaContraparte: 'Proveedor',
            ),
          if (resumen.sugerenciasCompra.isNotEmpty) ...[
            const _SeccionTitulo('Sugerencias de compra'),
            Card(
              child: Column(
                children: resumen.sugerenciasCompra
                    .map(
                      (s) => ListTile(
                        leading: const Icon(
                          Icons.shopping_cart_outlined,
                          color: _warning,
                        ),
                        title: Text('Producto #${s.productoId}'),
                        subtitle: Text(
                          'Existencia ${s.existenciaActual} · mínimo ${s.stockMinimo}',
                        ),
                        trailing: Text(
                          'Sugerido: ${s.cantidadSugerida}',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
          if (resumen.sugerenciasTraslado.isNotEmpty) ...[
            const _SeccionTitulo('Sugerencias de traslado'),
            Card(
              child: Column(
                children: resumen.sugerenciasTraslado
                    .map(
                      (s) => ListTile(
                        leading: const Icon(Icons.swap_horiz, color: _brand),
                        title: Text('Producto #${s.productoId}'),
                        subtitle: Text(
                          'Desde tienda #${s.tiendaOrigenId} (existencia ${s.existenciaOrigen})',
                        ),
                        trailing: Text(
                          'Sugerido: ${s.cantidadSugerida}',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
          const SizedBox(height: 12),
        ],
      ),
    );
  }
}

class _AlertasBanner extends StatelessWidget {
  const _AlertasBanner({required this.resumen});

  final DashboardResumen resumen;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: resumen.alertasCriticas > 0
          ? const Color(0xFFFDECEA)
          : const Color(0xFFFFF8E1),
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              Icons.warning_amber_rounded,
              color: resumen.alertasCriticas > 0 ? _danger : _warning,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                '${resumen.alertasCriticas} alerta(s) crítica(s) · '
                '${resumen.alertasPreventivas} preventiva(s) sin leer',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SeccionTitulo extends StatelessWidget {
  const _SeccionTitulo(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 20, bottom: 10),
      child: Text(
        texto.toUpperCase(),
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: Colors.black45,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
    required this.titulo,
    required this.valor,
    this.subtitulo,
    this.colorValor = _primary,
  });

  final String titulo;
  final String valor;
  final String? subtitulo;
  final Color colorValor;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              titulo,
              style: const TextStyle(fontSize: 12, color: Colors.black54),
            ),
            const SizedBox(height: 4),
            Text(
              valor,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: colorValor,
              ),
            ),
            if (subtitulo != null) ...[
              const SizedBox(height: 2),
              Text(
                subtitulo!,
                style: const TextStyle(fontSize: 11, color: Colors.black45),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _AgingCard extends StatelessWidget {
  const _AgingCard({
    required this.a0a30,
    required this.a31a60,
    required this.aMas60,
  });

  final Decimal a0a30;
  final Decimal a31a60;
  final Decimal aMas60;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Antigüedad de saldo vencido',
              style: TextStyle(fontSize: 12, color: Colors.black54),
            ),
            const SizedBox(height: 6),
            _AgingRow(etiqueta: '0-30 días', valor: a0a30),
            _AgingRow(etiqueta: '31-60 días', valor: a31a60, color: _warning),
            _AgingRow(etiqueta: '+60 días', valor: aMas60, color: _danger),
          ],
        ),
      ),
    );
  }
}

class _AgingRow extends StatelessWidget {
  const _AgingRow({
    required this.etiqueta,
    required this.valor,
    this.color = Colors.black87,
  });

  final String etiqueta;
  final Decimal valor;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(etiqueta, style: const TextStyle(fontSize: 12)),
          Text(
            'Q $valor',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

class _ListaCuentasPendientes extends StatelessWidget {
  const _ListaCuentasPendientes({
    required this.titulo,
    required this.cuentas,
    required this.etiquetaContraparte,
  });

  final String titulo;
  final List<CuentaPendiente> cuentas;
  final String etiquetaContraparte;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            titulo,
            style: const TextStyle(fontSize: 12, color: Colors.black54),
          ),
          const SizedBox(height: 6),
          Card(
            child: Column(
              children: cuentas
                  .map(
                    (c) => ListTile(
                      dense: true,
                      title: Text('$etiquetaContraparte #${c.contraparteId}'),
                      subtitle: Text(
                        'Vence ${c.fechaVencimiento.toLocal().toString().split(' ').first}',
                      ),
                      trailing: Text(
                        'Q ${c.monto}',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ],
      ),
    );
  }
}
