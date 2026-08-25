import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/categoria.dart';
import '../data/categorias_api.dart';

final categoriasApiProvider = Provider<CategoriasApi>(
  (ref) => CategoriasApi(ApiClient.instance),
);

/// Solo categorías activas — el POS no debe ofrecer filtrar por una que ya
/// no está en uso. Requiere `CATEGORIAS_VER` (ver
/// `007-seed-categorias-ver-operativo.xml` en market-backend — sin ese seed,
/// CAJERO/ENCARGADO_TIENDA reciben 403 acá).
final categoriasProvider = FutureProvider<List<Categoria>>((ref) async {
  final categorias = await ref.watch(categoriasApiProvider).listar();
  return categorias.where((c) => c.activa).toList();
});

/// `null` = "Todos" — sin filtro de categoría.
class CategoriaSeleccionadaNotifier extends Notifier<int?> {
  @override
  int? build() => null;

  void seleccionar(int? categoriaId) => state = categoriaId;
}

final categoriaSeleccionadaProvider =
    NotifierProvider<CategoriaSeleccionadaNotifier, int?>(
      CategoriaSeleccionadaNotifier.new,
    );
