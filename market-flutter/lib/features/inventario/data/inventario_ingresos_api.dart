import '../../../core/network/api_client.dart';
import 'ingreso_producto.dart';

class InventarioIngresosApi {
  InventarioIngresosApi(this._client);

  final ApiClient _client;

  /// `tiendaId` es siempre la tienda propia del usuario (la que ya pasa el
  /// alcance normal vía `PermissionInterceptor`) — el backend resuelve el
  /// grupo de esa tienda y devuelve los últimos ingresos (compras) de cada
  /// tienda hermana, con costo y proveedor, aunque el usuario no tenga esas
  /// tiendas en su propio alcance individual (ver
  /// `InventarioServiceImpl.listarUltimosIngresosPorGrupo`, market-backend).
  Future<List<IngresoProducto>> obtenerPorGrupo({
    required int tiendaId,
    required int productoId,
  }) {
    return _client.get<List<IngresoProducto>>(
      '/api/v1/inventario/tiendas/$tiendaId/productos/$productoId/ingresos/grupo',
      parser: (data) => (data as List<dynamic>)
          .map((json) => IngresoProducto.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }
}
