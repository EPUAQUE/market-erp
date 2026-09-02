import 'package:isar_community/isar.dart';

part 'producto_catalogo_isar.g.dart';

/// Mirror local del catálogo de venta de una tienda. `id` es directamente el
/// `productoId` del backend (único por definición) para que guardar el
/// catálogo entero sea un simple `putAll` — nunca hay que buscar-antes-de-
/// insertar. Precio/existencia se guardan como `String` (igual que llegan del
/// backend) y se parsean a `Decimal` recién al leer, mismo criterio que el
/// resto del cliente.
@collection
class ProductoCatalogoIsar {
  Id get id => productoId;

  late int productoId;
  late int tiendaId;
  late String codigoInterno;
  String? codigoBarras;
  late String nombre;
  String? descripcionCorta;
  String? imagenUrl;
  late String precioVenta;
  late String existenciaActual;
  late bool permitirVenta;
  int? categoriaId;
}
