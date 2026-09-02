import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/ventas/domain/carrito.dart';

LineaCarrito _linea({
  int productoId = 1,
  String nombre = 'Producto',
  String precioUnitario = '10.00',
  int cantidad = 1,
}) {
  return LineaCarrito(
    productoId: productoId,
    nombre: nombre,
    precioUnitario: Decimal.parse(precioUnitario),
    cantidad: cantidad,
  );
}

void main() {
  group('LineaCarrito', () {
    test('subtotal multiplica precio por cantidad', () {
      final linea = _linea(precioUnitario: '8.50', cantidad: 3);
      expect(linea.subtotal, Decimal.parse('25.50'));
    });

    test('conCantidad devuelve una copia con la nueva cantidad', () {
      final linea = _linea(cantidad: 2);
      final actualizada = linea.conCantidad(5);
      expect(actualizada.cantidad, 5);
      expect(actualizada.productoId, linea.productoId);
      expect(linea.cantidad, 2);
    });
  });

  group('CarritoState.agregar', () {
    test('agrega una línea nueva cuando el producto no está en el carrito', () {
      const carrito = CarritoState();
      final resultado = carrito.agregar(_linea(productoId: 1));
      expect(resultado.lineas, hasLength(1));
    });

    test('suma la cantidad cuando el producto ya está en el carrito', () {
      final carrito = const CarritoState().agregar(
        _linea(productoId: 1, cantidad: 2),
      );
      final resultado = carrito.agregar(_linea(productoId: 1, cantidad: 3));
      expect(resultado.lineas, hasLength(1));
      expect(resultado.lineas.single.cantidad, 5);
    });
  });

  group('CarritoState.actualizarCantidad', () {
    test('actualiza la cantidad de la línea indicada', () {
      final carrito = const CarritoState().agregar(_linea(productoId: 1));
      final resultado = carrito.actualizarCantidad(1, 7);
      expect(resultado.lineas.single.cantidad, 7);
    });

    test('cantidad cero quita la línea en vez de dejarla en 0', () {
      final carrito = const CarritoState().agregar(_linea(productoId: 1));
      final resultado = carrito.actualizarCantidad(1, 0);
      expect(resultado.lineas, isEmpty);
    });

    test('cantidad negativa también quita la línea', () {
      final carrito = const CarritoState().agregar(_linea(productoId: 1));
      final resultado = carrito.actualizarCantidad(1, -1);
      expect(resultado.lineas, isEmpty);
    });
  });

  group('CarritoState.quitar / vaciar', () {
    test('quitar elimina solo la línea del producto indicado', () {
      final carrito = const CarritoState()
          .agregar(_linea(productoId: 1))
          .agregar(_linea(productoId: 2));
      final resultado = carrito.quitar(1);
      expect(resultado.lineas, hasLength(1));
      expect(resultado.lineas.single.productoId, 2);
    });

    test('vaciar deja el carrito vacío', () {
      final carrito = const CarritoState().agregar(_linea(productoId: 1));
      expect(carrito.vaciar().estaVacio, isTrue);
    });
  });

  group('CarritoState.total / estaVacio', () {
    test('total suma el subtotal de todas las líneas', () {
      final carrito = const CarritoState()
          .agregar(_linea(productoId: 1, precioUnitario: '10.00', cantidad: 2))
          .agregar(_linea(productoId: 2, precioUnitario: '5.50', cantidad: 1));
      expect(carrito.total, Decimal.parse('25.50'));
    });

    test('estaVacio es true sin líneas', () {
      expect(const CarritoState().estaVacio, isTrue);
    });
  });

  group('calcularCambio', () {
    test('devuelve la diferencia cuando el monto alcanza', () {
      final cambio = calcularCambio(
        total: Decimal.parse('20.00'),
        montoRecibido: Decimal.parse('25.00'),
      );
      expect(cambio, Decimal.parse('5.00'));
    });

    test('devuelve cero exacto cuando el monto es igual al total', () {
      final cambio = calcularCambio(
        total: Decimal.parse('20.00'),
        montoRecibido: Decimal.parse('20.00'),
      );
      expect(cambio, Decimal.zero);
    });

    test(
      'devuelve null cuando el monto no alcanza — nunca un cambio negativo',
      () {
        final cambio = calcularCambio(
          total: Decimal.parse('20.00'),
          montoRecibido: Decimal.parse('19.99'),
        );
        expect(cambio, isNull);
      },
    );
  });
}
