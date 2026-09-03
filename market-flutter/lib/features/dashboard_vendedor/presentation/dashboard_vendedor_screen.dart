import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard/application/dashboard_provider.dart';
import '../../dashboard/data/dashboard_resumen.dart';
import '../../dashboard/presentation/dashboard_widgets.dart';
import '../../../core/theme/theme_notifier.dart';

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
    final modoOscuro = ref.watch(themeModeProvider) == ThemeMode.dark;

    return Scaffold(
      backgroundColor: DashboardPalette.surface,
      appBar: AppBar(
        backgroundColor: DashboardPalette.brand,
        foregroundColor: Colors.white,
        title: const Text('Mi rendimiento'),
        actions: [
          IconButton(
            icon: Icon(
              modoOscuro ? Icons.light_mode_outlined : Icons.dark_mode_outlined,
            ),
            tooltip: modoOscuro ? 'Modo claro' : 'Modo oscuro',
            onPressed: () => ref.read(themeModeProvider.notifier).alternar(),
          ),
        ],
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
                child: DashboardStatCard(
                  titulo: 'Ventas hoy',
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
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(18),
              gradient: LinearGradient(
                colors: [
                  DashboardPalette.accent.withValues(alpha: 0.16),
                  DashboardPalette.accent.withValues(alpha: 0.04),
                ],
              ),
              border: Border.all(
                color: DashboardPalette.accent.withValues(alpha: 0.3),
              ),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const DashboardIconBadge(
                  icon: Icons.info_outline_rounded,
                  color: DashboardPalette.accent,
                  size: 36,
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Meta de ventas y ranking interno todavía no están '
                    'disponibles: el sistema no registra una meta por '
                    'vendedor ni separa las ventas por quién las hizo. '
                    'Las cifras de arriba son las de toda la tienda.',
                    style: TextStyle(fontSize: 12, color: DashboardPalette.ink),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
