import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/cliente.dart';
import '../data/clientes_api.dart';

final clientesApiProvider = Provider<ClientesApi>(
  (ref) => ClientesApi(ApiClient.instance),
);

final clientesProvider = FutureProvider.autoDispose<List<Cliente>>((ref) {
  return ref.watch(clientesApiProvider).listar();
});
