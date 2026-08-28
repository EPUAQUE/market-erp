import '../../features/caja/data/movimiento_caja_pendiente_local.dart';
import '../../features/clientes/data/cliente_pendiente_local.dart';
import '../../features/productos/data/producto_catalogo.dart';
import '../../features/ventas/data/venta_pendiente_local.dart';

/// Puerto de almacenamiento local — nunca importar Isar directamente fuera de
/// `local_store_io.dart`. Los tipos anotados `@collection` de Isar generan
/// IDs de esquema como enteros de 64 bits que dart2js/DDC no puede
/// representar exactamente (error de compilación, no solo warning), así que
/// esos archivos no pueden formar parte del grafo de compilación web bajo
/// ningún camino de import, ni siquiera detrás de un `if (kIsWeb)` en
/// tiempo de ejecución. `local_store_web.dart` es la implementación que sí
/// puede compilar en web: no toca Isar, se limita a decir "no hay caché".
abstract class LocalStore {
  /// `false` en web (no hay mirror local ahí — ver CLAUDE.md). El resto de
  /// los métodos son no-op seguros cuando esto es `false`.
  bool get disponible;

  Future<void> guardarCatalogo(int tiendaId, List<ProductoCatalogo> productos);

  Future<List<ProductoCatalogo>> leerCatalogo(int tiendaId);

  Future<void> encolarVentaPendiente(NuevaVentaPendiente venta);

  Future<List<VentaPendienteLocal>> listarVentasPendientes();

  Future<void> marcarVentaPendienteConError(int id, String mensaje);

  /// Ítems con `mensajeError` no-nulo — el motor de sync nunca los reintenta
  /// solo; existen para que una pantalla los muestre y un encargado decida.
  Future<List<VentaPendienteLocal>> listarVentasPendientesConError();

  /// Limpia `mensajeError` para que el próximo drenado la vuelva a intentar
  /// como cualquier ítem nuevo.
  Future<void> reintentarVentaPendiente(int id);

  Future<void> eliminarVentaPendiente(int id);

  Future<int> contarVentasPendientes();

  Future<void> encolarMovimientoCajaPendiente(
    NuevoMovimientoCajaPendiente movimiento,
  );

  Future<List<MovimientoCajaPendienteLocal>> listarMovimientosCajaPendientes();

  Future<void> marcarMovimientoCajaPendienteConError(int id, String mensaje);

  Future<List<MovimientoCajaPendienteLocal>>
  listarMovimientosCajaPendientesConError();

  Future<void> reintentarMovimientoCajaPendiente(int id);

  Future<void> eliminarMovimientoCajaPendiente(int id);

  Future<int> contarMovimientosCajaPendientes();

  /// Devuelve el id local (Isar) asignado — una venta offline lo usa como
  /// `clientePendienteLocalId` para referenciar este cliente antes de que
  /// tenga id real de servidor (ver `ClienteSelectorSheet`).
  Future<int> encolarClientePendiente(NuevoClientePendiente cliente);

  /// Solo los que ni fallaron ni ya sincronizaron (`clienteServidorId` nulo)
  /// — un cliente ya sincronizado se conserva en Isar (ver
  /// `marcarClientePendienteSincronizado`) pero no debe reintentarse.
  Future<List<ClientePendienteLocal>> listarClientesPendientes();

  Future<void> marcarClientePendienteConError(int id, String mensaje);

  /// A diferencia de `eliminarClientePendiente`, esto NO borra la fila: una
  /// venta que todavía la referencia por `clientePendienteLocalId` necesita
  /// poder leer `clienteServidorId` después de que este cliente sincronice.
  Future<void> marcarClientePendienteSincronizado(
    int id,
    int clienteServidorId,
  );

  /// Usado por `SyncEngineNotifier._sincronizarVenta` para resolver el
  /// `clienteServidorId` de una venta que referencia un cliente por
  /// `clientePendienteLocalId`. `null` si el id no existe (no debería pasar).
  Future<ClientePendienteLocal?> obtenerClientePendiente(int id);

  Future<List<ClientePendienteLocal>> listarClientesPendientesConError();

  Future<void> reintentarClientePendiente(int id);

  Future<void> eliminarClientePendiente(int id);

  Future<int> contarClientesPendientes();

  /// Borra los clientes ya sincronizados (`clienteServidorId` no nulo) que
  /// ninguna venta pendiente sigue referenciando por
  /// `clientePendienteLocalId` — la fila de mapeo que
  /// `marcarClientePendienteSincronizado` conserva a propósito deja de
  /// hacer falta en cuanto sincroniza la última venta que la necesitaba.
  /// Minimiza cuánto tiempo vive en el dispositivo el nombre/teléfono/NIT
  /// de un cliente dado de alta offline — llamado al final de cada
  /// drenado (`SyncEngineNotifier`).
  Future<void> limpiarClientesPendientesSincronizadosSinReferencia();

  /// Borra catálogo y las 3 colas pendientes. Llamado solo al cerrar sesión
  /// — nunca antes de confirmar que no hay pendientes sin sincronizar (ver
  /// `AuthNotifier.logout`), para no perder ventas/movimientos/clientes
  /// reales que un usuario haya generado offline.
  Future<void> limpiarTodo();
}
