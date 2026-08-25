import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../auth/application/auth_notifier.dart';
import '../../dashboard_encargado/presentation/dashboard_encargado_screen.dart';
import '../../dashboard_vendedor/presentation/dashboard_vendedor_screen.dart';

/// Punto de entrada único para `/dashboard` — elige la vista según el rol
/// real del usuario. `CAJA_VER` ya se usa en `PosScreen` como el permiso que
/// distingue ENCARGADO de VENDEDOR/CAJERO (ver CLAUDE.md); se reutiliza el
/// mismo criterio acá en vez de inventar un segundo chequeo de rol.
class DashboardRouterScreen extends ConsumerWidget {
  const DashboardRouterScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sesion = ref.watch(authNotifierProvider).value;
    final esEncargado = sesion?.can('CAJA_VER') ?? false;
    return esEncargado
        ? const DashboardEncargadoScreen()
        : const DashboardVendedorScreen();
  }
}
