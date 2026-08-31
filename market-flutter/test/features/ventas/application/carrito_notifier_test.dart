import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/productos/data/producto_catalogo.dart';
import 'package:market_pos/features/ventas/application/carrito_notifier.dart';

ProductoCatalogo _producto({int productoId = 1, String precio = '8.50'}) {
  return ProductoCatalogo(
    productoId: productoId,
    codigoInterno: 'P00$productoId',
    codigoBarras: null,
    nombre: 'Producto $productoId',
    imagenUrl: null,
    precioVenta: Decimal.parse(precio),
    existenciaActual: Decimal.parse('10'),
    permitirVenta: true,
    categoriaId: 1,
  );
}

void main() {
  late ProviderContainer container;

  setUp(() => container = ProviderContainer());
  tearDown(() => container.dispose());

  test('agregarProducto agrega una línea con cantidad 1 por defecto', () {
    container.read(carritoProvider.notifier).agregarProducto(_producto());
    final estado = container.read(carritoProvider);
    expect(estado.lineas, hasLength(1));
    expect(estado.lineas.single.cantidad, Decimal.one);
  });

  test('agregarProducto dos veces el mismo producto suma cantidades', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto(productoId: 1));
    notifier.agregarProducto(_producto(productoId: 1));
    final estado = container.read(carritoProvider);
    expect(estado.lineas, hasLength(1));
    expect(estado.lineas.single.cantidad, Decimal.parse('2'));
  });

  test('incrementar/decrementar mutan la línea correcta', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto(productoId: 1));
    notifier.agregarProducto(_producto(productoId: 2));

    notifier.incrementar(2);
    expect(
      container
          .read(carritoProvider)
          .lineas
          .firstWhere((l) => l.productoId == 2)
          .cantidad,
      Decimal.parse('2'),
    );
    expect(
      container
          .read(carritoProvider)
          .lineas
          .firstWhere((l) => l.productoId == 1)
          .cantidad,
      Decimal.one,
    );

    notifier.decrementar(2);
    expect(
      container
          .read(carritoProvider)
          .lineas
          .firstWhere((l) => l.productoId == 2)
          .cantidad,
      Decimal.one,
    );
  });

  test('decrementar hasta cero quita la línea', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto(productoId: 1));
    notifier.decrementar(1);
    expect(container.read(carritoProvider).estaVacio, isTrue);
  });

  test('quitar elimina la línea sin afectar las demás', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto(productoId: 1));
    notifier.agregarProducto(_producto(productoId: 2));
    notifier.quitar(1);
    final estado = container.read(carritoProvider);
    expect(estado.lineas, hasLength(1));
    expect(estado.lineas.single.productoId, 2);
  });

  test('vaciar deja el carrito vacío', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto());
    notifier.vaciar();
    expect(container.read(carritoProvider).estaVacio, isTrue);
  });

  test('actualizarCantidad refleja el nuevo total', () {
    final notifier = container.read(carritoProvider.notifier);
    notifier.agregarProducto(_producto(productoId: 1, precio: '10.00'));
    notifier.actualizarCantidad(1, Decimal.parse('3'));
    expect(container.read(carritoProvider).total, Decimal.parse('30.00'));
  });
}
