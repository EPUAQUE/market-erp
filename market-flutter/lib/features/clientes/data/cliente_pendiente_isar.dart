import 'package:isar_community/isar.dart';

part 'cliente_pendiente_isar.g.dart';

/// Un cliente nuevo dado de alta sin conexión, en cola para sincronizar.
/// `id` es el autoIncrement de Isar — puramente local, nunca se manda al
/// backend (el cliente real recibe su propio id ahí al sincronizar). No se
/// puede usar como `clienteId` de una venta en la misma sesión offline — ver
/// `ClienteSelectorSheet` y CLAUDE.md.
@collection
class ClientePendienteIsar {
  Id id = Isar.autoIncrement;

  late String nombre;
  String? telefono;
  String? nit;
  String? limiteCredito;
  late DateTime creadaEn;

  /// Mismo contrato que `VentaPendienteIsar.mensajeError`.
  String? mensajeError;
}
