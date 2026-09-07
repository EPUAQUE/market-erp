import '../../../core/network/api_client.dart';
import 'proveedor.dart';

class ProveedoresApi {
  ProveedoresApi(this._client);

  final ApiClient _client;

  /// A diferencia de Clientes/Productos/Cuentas por Cobrar, este endpoint
  /// devuelve un array plano — nunca pasó por el rollout de paginación del
  /// backend (ver `market-backend`, `ProveedorController`), así que no hay
  /// envelope `{contenido, ...}` que desenvolver acá.
  Future<List<Proveedor>> listar() {
    return _client.get<List<Proveedor>>(
      '/api/v1/proveedores',
      parser: (data) => (data as List<dynamic>)
          .map((json) => Proveedor.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }
}
