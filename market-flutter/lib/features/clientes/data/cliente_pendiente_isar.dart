import 'package:isar_community/isar.dart';

part 'cliente_pendiente_isar.g.dart';

/// Un cliente nuevo dado de alta sin conexión, en cola para sincronizar.
/// `id` es el autoIncrement de Isar — puramente local, nunca se manda al
/// backend (el cliente real recibe su propio id ahí al sincronizar). Una
/// venta offline SÍ puede referenciar este cliente antes de que sincronice
/// (`VentaPendienteIsar.clientePendienteLocalId`) — ver `ClienteSelectorSheet`
/// y `SyncEngineNotifier._sincronizarVenta`.
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

  /// `null` mientras no se haya sincronizado. Una vez sincronizado, la fila
  /// se conserva (no se borra) precisamente para que esta columna siga
  /// resolviendo el id real a cualquier venta que todavía la referencie por
  /// `clientePendienteLocalId` — borrarla de inmediato perdería ese mapeo si
  /// el drenado se interrumpe entre sincronizar el cliente y sincronizar la
  /// venta que lo usa.
  int? clienteServidorId;
}
