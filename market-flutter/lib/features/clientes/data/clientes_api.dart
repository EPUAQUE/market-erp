import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import 'cliente.dart';

class ClientesApi {
  ClientesApi(this._client);

  final ApiClient _client;

  Future<List<Cliente>> listar() {
    return _client.get<List<Cliente>>(
      '/api/v1/clientes',
      parser: (data) => (data as List<dynamic>)
          .map((json) => Cliente.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<Cliente> crear({
    required String nombre,
    String? telefono,
    String? nit,
    Decimal? limiteCredito,
  }) {
    return _client.post<Cliente>(
      '/api/v1/clientes',
      data: {
        'nombre': nombre,
        'telefono': telefono,
        'nit': nit,
        'limiteCredito': limiteCredito?.toString(),
      },
      parser: (data) => Cliente.fromJson(data as Map<String, dynamic>),
    );
  }
}
