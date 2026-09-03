import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../auth/application/auth_notifier.dart';
import '../../auth/presentation/logout_confirm.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/theme_notifier.dart';
import '../../../shared/widgets/connectivity_badge.dart';
import '../application/modo_venta_rapida_provider.dart';
import 'pos/pos_body_telefono.dart';
import 'pos/pos_columna_accesos.dart';
import 'pos/pos_columna_carrito.dart';
import 'pos/pos_columna_productos.dart';

class _AccionPos {
  const _AccionPos({
    required this.icono,
    required this.etiqueta,
    required this.onTap,
  });

  final IconData icono;
  final String etiqueta;
  final VoidCallback onTap;
}

/// Pantalla principal del POS — "Nueva Venta", primera pantalla tras el
/// login (ver CLAUDE.md, "Product goal"). El cuerpo se divide en widgets
/// propios bajo `pos/` (columna de categorías, catálogo, carrito, layout de
/// teléfono) — este archivo solo arma el `Scaffold`/`AppBar` y decide qué
/// layout mostrar según el ancho disponible.
class PosScreen extends ConsumerWidget {
  const PosScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final sesion = ref.watch(authNotifierProvider).value;
    final puedeVerCaja = sesion?.can('CAJA_VER') ?? false;
    final puedeVerDashboard = sesion?.can('DASHBOARD_VER') ?? false;
    final puedeVerCuentasPorCobrar =
        sesion?.can('CUENTAS_POR_COBRAR_VER') ?? false;
    final modoRapido = ref.watch(modoVentaRapidaProvider);
    final modoOscuro = ref.watch(themeModeProvider) == ThemeMode.dark;
    final colors = AppColors.of(context);
    // Debajo de este ancho (teléfonos en portrait, ~411dp lógicos en un
    // Pixel de gama media) el título + badge + 5 botones de acción ya no
    // caben en una sola fila del AppBar — desbordan a la derecha. Esta app
    // es tablet-first (ver CLAUDE.md), pero debe seguir siendo usable sin
    // desbordar en una pantalla más angosta.
    final anchoAngosto = MediaQuery.sizeOf(context).width < 700;

    final accionesNavegacion = <_AccionPos>[
      if (puedeVerDashboard)
        _AccionPos(
          icono: Icons.dashboard_outlined,
          etiqueta: 'Dashboard',
          onTap: () => context.push('/dashboard'),
        ),
      if (puedeVerCaja)
        _AccionPos(
          icono: Icons.point_of_sale,
          etiqueta: 'Caja',
          onTap: () => context.push('/caja'),
        ),
      if (puedeVerCuentasPorCobrar)
        _AccionPos(
          icono: Icons.request_quote_outlined,
          etiqueta: 'Cuentas por cobrar',
          onTap: () => context.push('/cuentas-por-cobrar'),
        ),
      _AccionPos(
        icono: Icons.logout,
        etiqueta: 'Salir',
        onTap: () => cerrarSesionConConfirmacion(context, ref),
      ),
    ];

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
        title: const Text('POS · Nueva Venta'),
        actions: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Center(child: ConnectivityBadge()),
          ),
          IconButton(
            icon: Icon(modoRapido ? Icons.bolt : Icons.bolt_outlined),
            color: modoRapido ? colors.pending : null,
            tooltip: modoRapido
                ? 'Salir de Modo Venta Rápida'
                : 'Modo Venta Rápida',
            onPressed: () =>
                ref.read(modoVentaRapidaProvider.notifier).alternar(),
          ),
          IconButton(
            icon: Icon(
              modoOscuro ? Icons.light_mode_outlined : Icons.dark_mode_outlined,
            ),
            tooltip: modoOscuro ? 'Modo claro' : 'Modo oscuro',
            onPressed: () => ref.read(themeModeProvider.notifier).alternar(),
          ),
          if (anchoAngosto)
            PopupMenuButton<VoidCallback>(
              icon: const Icon(Icons.more_vert),
              onSelected: (accion) => accion(),
              itemBuilder: (context) => [
                for (final accion in accionesNavegacion)
                  PopupMenuItem(
                    value: accion.onTap,
                    child: Row(
                      children: [
                        Icon(accion.icono, size: 20, color: Colors.black54),
                        const SizedBox(width: 12),
                        Text(accion.etiqueta),
                      ],
                    ),
                  ),
              ],
            )
          else
            for (final accion in accionesNavegacion)
              IconButton(
                icon: Icon(accion.icono),
                tooltip: accion.etiqueta,
                onPressed: accion.onTap,
              ),
        ],
      ),
      body: anchoAngosto
          ? PosBodyTelefono(tiendaId: tiendaId, modoRapido: modoRapido)
          : Row(
              children: [
                if (!modoRapido) const ColumnaAccesos(),
                Expanded(
                  child: ColumnaProductos(
                    tiendaId: tiendaId,
                    modoRapido: modoRapido,
                  ),
                ),
                SizedBox(
                  width: 340,
                  child: ColumnaCarrito(modoRapido: modoRapido),
                ),
              ],
            ),
    );
  }
}
