import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_provider.dart';
import '../../dashboard/data/dashboard_resumen.dart';

const _brand = Color(0xFF0F4C5C);
const _primary = Color(0xFF2E8B57);

/// Rendimiento del vendedor. El backend agrega por TIENDA, no por vendedor
/// (no expone `usuarioId` en `/me` ni un corte por vendedor en ventas) — así
/// que "ventas hoy/mes" acá son las de toda la tienda, no solo las propias.
/// "Meta" y "ranking interno" del brief original no tienen ningún dato
/// server-side que los respalde todavía: se documentan como pendientes en vez
/// de inventar un número (ver CLAUDE.md, "Known backend gaps").
class DashboardVendedorScreen extends ConsumerWidget {
  const DashboardVendedorScreen({super.key});

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
        title: const Text('Mi rendimiento'),
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
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  titulo: 'Ventas hoy',
                  valor: 'Q ${resumen.ventasHoyTotal}',
                  subtitulo: '${resumen.ventasHoyCantidad} venta(s)',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _StatCard(
                  titulo: 'Ventas del mes',
                  valor: 'Q ${resumen.ventasMesTotal}',
                  subtitulo: '${resumen.ventasMesCantidad} venta(s)',
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _StatCard(
            titulo: 'Ticket promedio del mes',
            valor: 'Q ${resumen.ticketPromedioMes}',
          ),
          const SizedBox(height: 20),
          Card(
            color: const Color(0xFFFFF8E1),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Icon(Icons.info_outline, color: Colors.black54),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Meta de ventas y ranking interno todavía no están '
                      'disponibles: el sistema no registra una meta por '
                      'vendedor ni separa las ventas por quién las hizo. '
                      'Las cifras de arriba son las de toda la tienda.',
                      style: TextStyle(fontSize: 12, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.titulo, required this.valor, this.subtitulo});

  final String titulo;
  final String valor;
  final String? subtitulo;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(titulo, style: const TextStyle(color: Colors.black54)),
            const SizedBox(height: 6),
            Text(
              valor,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: _primary,
              ),
            ),
            if (subtitulo != null) ...[
              const SizedBox(height: 2),
              Text(
                subtitulo!,
                style: const TextStyle(fontSize: 12, color: Colors.black45),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
