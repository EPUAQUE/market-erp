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

  Future<void> encolarClientePendiente(NuevoClientePendiente cliente);

  Future<List<ClientePendienteLocal>> listarClientesPendientes();

  Future<void> marcarClientePendienteConError(int id, String mensaje);

  Future<List<ClientePendienteLocal>> listarClientesPendientesConError();

  Future<void> reintentarClientePendiente(int id);

  Future<void> eliminarClientePendiente(int id);

  Future<int> contarClientesPendientes();
}
