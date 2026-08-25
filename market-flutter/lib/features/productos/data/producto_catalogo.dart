import 'package:decimal/decimal.dart';

/// Combina tres fuentes del backend en una sola fila de catálogo de venta:
/// `GET /productos` (nombre/código/imagen), `GET /productos/tiendas/{id}`
/// (precio/permitirVenta) y `GET /inventario/tiendas/{id}` (existencia). El
/// backend no expone esto ya unido — se arma acá porque en el diseño
/// offline-first (ver CLAUDE.md) las tres listas se cachean localmente de
/// todos modos.
class ProductoCatalogo {
  const ProductoCatalogo({
    required this.productoId,
    required this.codigoInterno,
    required this.codigoBarras,
    required this.nombre,
    required this.imagenUrl,
    required this.precioVenta,
    required this.existenciaActual,
    required this.permitirVenta,
    required this.categoriaId,
  });

  final int productoId;
  final String codigoInterno;
  final String? codigoBarras;
  final String nombre;
  final String? imagenUrl;
  final Decimal precioVenta;
  final Decimal existenciaActual;
  final bool permitirVenta;

  /// Requerido en el backend (`Producto.categoriaId` es `@NotNull`), pero
  /// puede faltar acá si el `GET /productos` de este producto no se pudo
  /// resolver al armar el catálogo (ver `ProductosApi.listarCatalogo`) — en
  /// ese caso no se filtra por categoría, se muestra en "Todos".
  final int? categoriaId;

  bool get vendible => permitirVenta && existenciaActual > Decimal.zero;

  bool coincideBusqueda(String query) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return true;
    return nombre.toLowerCase().contains(q) ||
        codigoInterno.toLowerCase().contains(q) ||
        (codigoBarras?.toLowerCase().contains(q) ?? false);
  }

  bool coincideCodigoExacto(String codigo) {
    return codigoBarras == codigo || codigoInterno == codigo;
  }
}
