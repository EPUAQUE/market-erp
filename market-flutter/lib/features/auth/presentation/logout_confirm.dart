import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/sync/sync_engine.dart';
import '../application/auth_notifier.dart';

/// Único punto de entrada para cerrar sesión — nunca llamar
/// `authNotifierProvider.notifier.logout()` directamente desde una pantalla.
/// `logout()` borra el mirror local (Isar) completo, así que con
/// ventas/movimientos/clientes offline sin sincronizar, cerrar sesión los
/// perdería para siempre — en una tablet compartida, eso es perder ventas
/// reales, no solo el caché de catálogo.
///
/// Bloqueo duro, sin bypass (decisión explícita del usuario, PLAN_MEJORAS.md
/// Fase 2 parte C): antes se podía "cerrar sesión de todos modos" tras una
/// advertencia; ahora, con algo pendiente, `logout()` simplemente no se
/// llama — el único camino de salida es sincronizar (conectarse) o, para un
/// ítem realmente atascado, descartarlo explícitamente desde
/// `PendientesErrorScreen` (que ya pide su propia confirmación de "no se
/// puede deshacer" antes de borrar nada).
Future<void> cerrarSesionConConfirmacion(
  BuildContext context,
  WidgetRef ref,
) async {
  final pendientes = await ref.read(pendientesSincronizarProvider.future);
  if (pendientes > 0) {
    if (!context.mounted) return;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('No puedes cerrar sesión todavía'),
        content: Text(
          'Tienes $pendientes elemento(s) sin sincronizar (ventas, '
          'movimientos de caja o clientes). Cerrar sesión ahora los '
          'perdería permanentemente, así que no está permitido mientras '
          'sigan pendientes.\n\n'
          'Conéctate a internet para que sincronicen solos, o revisa '
          '"Pendientes con error" si alguno quedó atascado — desde ahí se '
          'puede reintentar o descartar uno explícitamente.',
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              dialogContext.push('/pendientes-error');
            },
            child: const Text('Ver pendientes'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
    return;
  }
  await ref.read(authNotifierProvider.notifier).logout();
}
