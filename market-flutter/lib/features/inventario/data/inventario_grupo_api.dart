import '../../../core/network/api_client.dart';
import 'existencia_tienda.dart';

class InventarioGrupoApi {
  InventarioGrupoApi(this._client);

  final ApiClient _client;

  /// `tiendaId` es siempre la tienda propia del usuario (la que ya pasa el
  /// alcance normal vía `PermissionInterceptor`) — el backend resuelve el
  /// grupo de esa tienda y devuelve la existencia en cada tienda hermana,
  /// aunque el usuario no tenga esas tiendas en su propio alcance individual
  /// (ver `InventarioServiceImpl.listarExistenciaPorGrupo`, market-backend).
  Future<List<ExistenciaTienda>> obtenerPorGrupo({
    required int tiendaId,
    required int productoId,
  }) {
    return _client.get<List<ExistenciaTienda>>(
      '/api/v1/inventario/tiendas/$tiendaId/productos/$productoId/grupo',
      parser: (data) => (data as List<dynamic>)
          .map(
            (json) => ExistenciaTienda.fromJson(json as Map<String, dynamic>),
          )
          .toList(),
    );
  }
}
