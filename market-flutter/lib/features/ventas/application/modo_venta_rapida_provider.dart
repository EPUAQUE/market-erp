import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Modo Venta Rápida — para horas pico (ver CLAUDE.md, "Product goal").
/// Estado puramente de sesión: no se persiste entre reinicios, se activa por
/// turno cuando arranca la fila. Solo cambia presentación (densidad de grid,
/// qué se muestra, duración de transiciones) — nunca reglas de negocio.
class ModoVentaRapidaNotifier extends Notifier<bool> {
  @override
  bool build() => false;

  void alternar() => state = !state;
}

final modoVentaRapidaProvider = NotifierProvider<ModoVentaRapidaNotifier, bool>(
  ModoVentaRapidaNotifier.new,
);
