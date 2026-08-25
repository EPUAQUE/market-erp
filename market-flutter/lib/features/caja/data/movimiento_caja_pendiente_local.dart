import 'package:decimal/decimal.dart';
import 'caja.dart';

/// Espejo plano (sin Isar) de un movimiento de caja encolado para
/// sincronizar — mismo patrón que `VentaPendienteLocal` (ver
/// `venta_pendiente_local.dart`).
class MovimientoCajaPendienteLocal {
  const MovimientoCajaPendienteLocal({
    required this.id,
    required this.tiendaId,
    required this.tipo,
    required this.concepto,
    required this.monto,
    required this.creadaEn,
    required this.mensajeError,
  });

  final int id;
  final int tiendaId;
  final TipoMovimientoCaja tipo;
  final String concepto;
  final Decimal monto;
  final DateTime creadaEn;
  final String? mensajeError;
}

/// Datos de un movimiento nuevo por encolar — sin `id` todavía.
class NuevoMovimientoCajaPendiente {
  const NuevoMovimientoCajaPendiente({
    required this.tiendaId,
    required this.tipo,
    required this.concepto,
    required this.monto,
    required this.creadaEn,
  });

  final int tiendaId;
  final TipoMovimientoCaja tipo;
  final String concepto;
  final Decimal monto;
  final DateTime creadaEn;
}
