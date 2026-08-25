import '../../../core/network/api_client.dart';
import 'categoria.dart';

class CategoriasApi {
  CategoriasApi(this._client);

  final ApiClient _client;

  Future<List<Categoria>> listar() {
    return _client.get<List<Categoria>>(
      '/api/v1/categorias',
      parser: (data) => (data as List<dynamic>)
          .map((json) => Categoria.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }
}
