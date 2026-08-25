import 'package:isar_community/isar.dart';

part 'movimiento_caja_pendiente_isar.g.dart';

/// Un movimiento de caja (ingreso/egreso) registrado sin conexión, en cola
/// para sincronizar. `id` es el autoIncrement de Isar — puramente local,
/// nunca se manda al backend.
@collection
class MovimientoCajaPendienteIsar {
  Id id = Isar.autoIncrement;

  late int tiendaId;

  /// Nombre del enum `TipoMovimientoCaja` (`ingreso`/`egreso`).
  late String tipo;

  late String concepto;
  late String monto;
  late DateTime creadaEn;

  /// Mismo contrato que `VentaPendienteIsar.mensajeError` — `null` = sigue
  /// pendiente/reintentable, no-nulo = fallo de negocio para revisión manual.
  String? mensajeError;
}
