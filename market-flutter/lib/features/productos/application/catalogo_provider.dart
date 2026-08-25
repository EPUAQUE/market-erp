import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/db/local_store_provider.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../data/producto_catalogo.dart';
import '../data/productos_api.dart';
import 'categorias_provider.dart';

final productosApiProvider = Provider<ProductosApi>(
  (ref) => ProductosApi(ApiClient.instance),
);

/// Catálogo de venta de la tienda activa — local-first en la práctica: si hay
/// red, trae el catálogo fresco y lo guarda en el `LocalStore` de paso; si la
/// red falla (no hay conexión), cae al último catálogo cacheado en vez de
/// dejar el POS sin poder vender. Sin caché y sin red, el error original sí
/// se propaga — no hay nada que mostrar.
final catalogoProvider = FutureProvider.family<List<ProductoCatalogo>, int>((
  ref,
  tiendaId,
) async {
  final store = await ref.watch(localStoreProvider.future);
  try {
    final productos = await ref
        .watch(productosApiProvider)
        .listarCatalogo(tiendaId);
    if (store.disponible) await store.guardarCatalogo(tiendaId, productos);
    return productos;
  } on ApiException catch (error) {
    if (!error.isNetworkError || !store.disponible) rethrow;
    final cache = await store.leerCatalogo(tiendaId);
    if (cache.isEmpty) rethrow;
    return cache;
  }
});

class BusquedaProductoNotifier extends Notifier<String> {
  @override
  String build() => '';

  void actualizar(String texto) => state = texto;

  void limpiar() => state = '';
}

final busquedaProductoProvider =
    NotifierProvider<BusquedaProductoNotifier, String>(
      BusquedaProductoNotifier.new,
    );

/// Resultado de aplicar la búsqueda + categoría seleccionada sobre el
/// catálogo ya cargado — separado del `FutureProvider` para no relanzar las
/// 3 llamadas de red en cada tecla o cada cambio de categoría.
final productosFiltradosProvider =
    Provider.family<AsyncValue<List<ProductoCatalogo>>, int>((ref, tiendaId) {
      final catalogo = ref.watch(catalogoProvider(tiendaId));
      final busqueda = ref.watch(busquedaProductoProvider);
      final categoriaId = ref.watch(categoriaSeleccionadaProvider);
      return catalogo.whenData(
        (items) => items
            .where((p) => p.coincideBusqueda(busqueda))
            .where((p) => categoriaId == null || p.categoriaId == categoriaId)
            .toList(),
      );
    });
