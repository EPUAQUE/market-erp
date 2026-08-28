import '../../features/caja/data/movimiento_caja_pendiente_local.dart';
import '../../features/clientes/data/cliente_pendiente_local.dart';
import '../../features/productos/data/producto_catalogo.dart';
import '../../features/ventas/data/venta_pendiente_local.dart';
import 'local_store.dart';

/// No importa Isar en absoluto — es justamente el punto (ver
/// `local_store.dart`). El navegador ya asume conexión al servidor que lo
/// sirve, así que no hace falta un mirror local en esta plataforma.
Future<LocalStore> crearLocalStore() async => const WebLocalStore();

class WebLocalStore implements LocalStore {
  const WebLocalStore();

  @override
  bool get disponible => false;

  @override
  Future<void> guardarCatalogo(
    int tiendaId,
    List<ProductoCatalogo> productos,
  ) async {}

  @override
  Future<List<ProductoCatalogo>> leerCatalogo(int tiendaId) async => const [];

  @override
  Future<void> encolarVentaPendiente(NuevaVentaPendiente venta) async {
    throw UnsupportedError(
      'No hay almacenamiento local disponible en web para encolar ventas offline.',
    );
  }

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientes() async => const [];

  @override
  Future<void> marcarVentaPendienteConError(int id, String mensaje) async {}

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientesConError() async =>
      const [];

  @override
  Future<void> reintentarVentaPendiente(int id) async {}

  @override
  Future<void> eliminarVentaPendiente(int id) async {}

  @override
  Future<int> contarVentasPendientes() async => 0;

  @override
  Future<void> encolarMovimientoCajaPendiente(
    NuevoMovimientoCajaPendiente movimiento,
  ) async {
    throw UnsupportedError(
      'No hay almacenamiento local disponible en web para encolar movimientos de caja offline.',
    );
  }

  @override
  Future<List<MovimientoCajaPendienteLocal>>
  listarMovimientosCajaPendientes() async => const [];

  @override
  Future<void> marcarMovimientoCajaPendienteConError(
    int id,
    String mensaje,
  ) async {}

  @override
  Future<List<MovimientoCajaPendienteLocal>>
  listarMovimientosCajaPendientesConError() async => const [];

  @override
  Future<void> reintentarMovimientoCajaPendiente(int id) async {}

  @override
  Future<void> eliminarMovimientoCajaPendiente(int id) async {}

  @override
  Future<int> contarMovimientosCajaPendientes() async => 0;

  @override
  Future<int> encolarClientePendiente(NuevoClientePendiente cliente) async {
    throw UnsupportedError(
      'No hay almacenamiento local disponible en web para encolar clientes offline.',
    );
  }

  @override
  Future<List<ClientePendienteLocal>> listarClientesPendientes() async =>
      const [];

  @override
  Future<void> marcarClientePendienteConError(int id, String mensaje) async {}

  @override
  Future<void> marcarClientePendienteSincronizado(
    int id,
    int clienteServidorId,
  ) async {}

  @override
  Future<ClientePendienteLocal?> obtenerClientePendiente(int id) async => null;

  @override
  Future<List<ClientePendienteLocal>> listarClientesPendientesConError() async =>
      const [];

  @override
  Future<void> reintentarClientePendiente(int id) async {}

  @override
  Future<void> eliminarClientePendiente(int id) async {}

  @override
  Future<int> contarClientesPendientes() async => 0;

  @override
  Future<void> limpiarTodo() async {}
}
