import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/proveedor.dart';
import '../data/proveedores_api.dart';

final proveedoresApiProvider = Provider<ProveedoresApi>(
  (ref) => ProveedoresApi(ApiClient.instance),
);

final proveedoresProvider = FutureProvider.autoDispose<List<Proveedor>>((ref) {
  return ref.watch(proveedoresApiProvider).listar();
});
