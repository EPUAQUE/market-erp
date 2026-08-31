import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/connectivity/connectivity_provider.dart';
import '../../core/sync/sync_engine.dart';

/// Indicador de conectividad — siempre visible, nunca bloquea la interacción
/// (ver CLAUDE.md, "Flujos offline"). Observarlo aquí es lo que mantiene vivo
/// [SyncEngineNotifier] mientras el POS está en pantalla, para que el drenado
/// de la cola dispare solo al reconectar. Tocable: lleva a la lista de
/// pendientes con error (los que el motor de sync nunca reintenta solo).
class ConnectivityBadge extends ConsumerWidget {
  const ConnectivityBadge({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(syncEngineProvider);
    final pendientes = ref.watch(pendientesSincronizarProvider).value ?? 0;
    final conError = ref.watch(pendientesConErrorProvider).value?.length ?? 0;

    final (color, texto) = switch (estado) {
      EstadoConexion.conectado => (const Color(0xFF3BAA68), 'Conectado'),
      EstadoConexion.sincronizando => (
        const Color(0xFFF4B942),
        'Sincronizando',
      ),
      EstadoConexion.sinConexion => (const Color(0xFFDC6B6B), 'Sin conexión'),
    };

    return Tooltip(
      message: conError > 0
          ? '$conError pendiente(s) con error — toca para revisar'
          : pendientes > 0
          ? '$pendientes elemento(s) pendiente(s) de sincronizar'
          : texto,
      child: InkWell(
        borderRadius: BorderRadius.circular(6),
        onTap: () => context.push('/pendientes-error'),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 10,
                height: 10,
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              ),
              const SizedBox(width: 6),
              Text(
                texto,
                style: const TextStyle(color: Colors.white, fontSize: 13),
              ),
              if (pendientes > 0) ...[
                const SizedBox(width: 6),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 1,
                  ),
                  decoration: BoxDecoration(
                    color: conError > 0
                        ? const Color(0xFFDC6B6B)
                        : Colors.white24,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$pendientes',
                    style: const TextStyle(color: Colors.white, fontSize: 11),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
