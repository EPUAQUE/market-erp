import 'package:isar_community/isar.dart';

part 'venta_pendiente_isar.g.dart';

@embedded
class LineaCarritoIsar {
  late int productoId;
  late String nombre;
  late String precioUnitario;
  late String cantidad;
}

/// Una venta creada sin conexión, en cola para sincronizar. `id` es el
/// autoIncrement de Isar — un identificador puramente local, nunca se manda
/// al backend (la venta real recibe su propio id ahí al sincronizar).
/// `correlationId` existe para una futura idempotencia server-side (el
/// backend no la soporta todavía — ver CLAUDE.md, "Known backend gaps"); por
/// ahora solo sirve para deduplicar client-side si el drenado se interrumpe
/// a mitad de una venta.
@collection
class VentaPendienteIsar {
  Id id = Isar.autoIncrement;

  late String correlationId;
  late int tiendaId;

  /// `null` cuando la venta referencia un cliente todavía no sincronizado —
  /// ver `clientePendienteLocalId`. Exactamente uno de los dos es no-nulo.
  int? clienteId;

  /// Id local (Isar) de un `ClientePendienteIsar` creado en la misma sesión
  /// offline, cuando el cliente elegido para esta venta todavía no tiene id
  /// real de servidor. `SyncEngineNotifier._sincronizarVenta` lo resuelve
  /// antes de mandar la venta — nunca se manda este id al backend.
  int? clientePendienteLocalId;

  late List<LineaCarritoIsar> lineas;

  /// Nombre del enum `MetodoPago` — solo para mostrarlo en el historial local,
  /// el backend no lo persiste (ver CLAUDE.md).
  late String metodoPago;

  /// `null` = venta 100% a crédito, sin cobro al sincronizar.
  String? montoACobrar;

  late DateTime creadaEn;

  /// `null` mientras no se haya intentado sincronizar o mientras siga en la
  /// cola por un fallo de red (reintentable). No-nulo = un fallo de negocio
  /// (ej. producto ya no vendible) que un encargado debe revisar — el motor
  /// de sync no la vuelve a intentar sola.
  String? mensajeError;
}
