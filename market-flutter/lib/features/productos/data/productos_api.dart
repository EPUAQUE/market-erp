import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/paginacion.dart';
import 'producto_catalogo.dart';

class ProductosApi {
  ProductosApi(this._client);

  final ApiClient _client;

  /// Combina las tres fuentes en una sola lista lista para el POS. Productos
  /// sin fila en `producto_tienda` para esta tienda (nunca asignados a ella)
  /// simplemente no aparecen — no son parte del catálogo de venta local.
  ///
  /// Los 3 endpoints ahora devuelven un envelope paginado (ver
  /// `core/network/paginacion.dart`) — el POS pide `tamanoPaginaCompleta`
  /// para preservar el "traer todo de una vez" que necesita para cachear
  /// offline, en vez de implementar un loop de páginas.
  Future<List<ProductoCatalogo>> listarCatalogo(int tiendaId) async {
    final results = await Future.wait([
      _client.get<List<dynamic>>(
        '/api/v1/productos',
        query: {'size': tamanoPaginaCompleta},
        parser: contenidoDePagina,
      ),
      _client.get<List<dynamic>>(
        '/api/v1/productos/tiendas/$tiendaId',
        query: {'size': tamanoPaginaCompleta},
        parser: contenidoDePagina,
      ),
      _client.get<List<dynamic>>(
        '/api/v1/inventario/tiendas/$tiendaId',
        query: {'size': tamanoPaginaCompleta},
        parser: contenidoDePagina,
      ),
    ]);

    final productos = {
      for (final json in results[0])
        (json as Map<String, dynamic>)['id'] as int: json,
    };
    final existencias = {
      for (final json in results[2])
        (json as Map<String, dynamic>)['productoId'] as int: Decimal.parse(
          json['existenciaActual'] as String,
        ),
    };

    return results[1].map((json) {
      final productoTienda = json as Map<String, dynamic>;
      final productoId = productoTienda['productoId'] as int;
      final producto = productos[productoId];
      return ProductoCatalogo(
        productoId: productoId,
        codigoInterno: producto?['codigoInterno'] as String? ?? '',
        codigoBarras: producto?['codigoBarras'] as String?,
        nombre: producto?['nombre'] as String? ?? 'Producto #$productoId',
        descripcionCorta: producto?['descripcionCorta'] as String?,
        imagenUrl: producto?['imagenUrl'] as String?,
        precioVenta: Decimal.parse(productoTienda['precioVenta'] as String),
        existenciaActual: existencias[productoId] ?? Decimal.zero,
        permitirVenta: productoTienda['permitirVenta'] as bool,
        categoriaId: producto?['categoriaId'] as int?,
      );
    }).toList();
  }
}
