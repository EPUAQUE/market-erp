import 'package:decimal/decimal.dart';

/// Espejo plano (sin Isar) de un cliente encolado para sincronizar — mismo
/// patrón que `VentaPendienteLocal` (ver `venta_pendiente_local.dart`).
class ClientePendienteLocal {
  const ClientePendienteLocal({
    required this.id,
    required this.nombre,
    required this.telefono,
    required this.nit,
    required this.limiteCredito,
    required this.creadaEn,
    required this.mensajeError,
    required this.clienteServidorId,
  });

  final int id;
  final String nombre;
  final String? telefono;
  final String? nit;
  final Decimal? limiteCredito;
  final DateTime creadaEn;
  final String? mensajeError;

  /// `null` mientras no se haya sincronizado — ver `ClientePendienteIsar`.
  final int? clienteServidorId;
}

/// Datos de un cliente nuevo por encolar — sin `id` todavía.
class NuevoClientePendiente {
  const NuevoClientePendiente({
    required this.nombre,
    required this.telefono,
    required this.nit,
    required this.limiteCredito,
    required this.creadaEn,
  });

  final String nombre;
  final String? telefono;
  final String? nit;
  final Decimal? limiteCredito;
  final DateTime creadaEn;
}
