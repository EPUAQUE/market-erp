import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/sync/sync_engine.dart';
import '../application/auth_notifier.dart';

const _danger = Color(0xFFDC6B6B);

/// Único punto de entrada para cerrar sesión — nunca llamar
/// `authNotifierProvider.notifier.logout()` directamente desde una pantalla.
/// `logout()` borra el mirror local (Isar) completo, así que si hay
/// ventas/movimientos/clientes offline sin sincronizar hay que avisar antes:
/// de lo contrario cerrar sesión en una tablet compartida perdería ventas
/// reales, no solo el caché de catálogo.
Future<void> cerrarSesionConConfirmacion(
  BuildContext context,
  WidgetRef ref,
) async {
  final pendientes = await ref.read(pendientesSincronizarProvider.future);
  if (pendientes > 0) {
    if (!context.mounted) return;
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Hay elementos sin sincronizar'),
        content: Text(
          'Tienes $pendientes elemento(s) pendientes de sincronizar '
          '(ventas, movimientos de caja o clientes). Si cierras sesión '
          'ahora, se perderán permanentemente.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: _danger),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Cerrar sesión de todos modos'),
          ),
        ],
      ),
    );
    if (confirmado != true) return;
  }
  await ref.read(authNotifierProvider.notifier).logout();
}
