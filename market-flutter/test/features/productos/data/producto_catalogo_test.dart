import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/productos/data/producto_catalogo.dart';

ProductoCatalogo _producto({
  int productoId = 1,
  String codigoInterno = 'P001',
  String? codigoBarras = '7501234567890',
  String nombre = 'Coca Cola 600ml',
  Decimal? existenciaActual,
  bool permitirVenta = true,
}) {
  return ProductoCatalogo(
    productoId: productoId,
    codigoInterno: codigoInterno,
    codigoBarras: codigoBarras,
    nombre: nombre,
    imagenUrl: null,
    precioVenta: Decimal.parse('8.50'),
    existenciaActual: existenciaActual ?? Decimal.parse('10'),
    permitirVenta: permitirVenta,
    categoriaId: 1,
  );
}

void main() {
  group('vendible', () {
    test('true cuando permitirVenta y hay existencia', () {
      expect(_producto().vendible, isTrue);
    });

    test('false sin existencia aunque permitirVenta sea true', () {
      expect(_producto(existenciaActual: Decimal.zero).vendible, isFalse);
    });

    test('false cuando permitirVenta es false aunque haya existencia', () {
      expect(_producto(permitirVenta: false).vendible, isFalse);
    });
  });

  group('coincideBusqueda', () {
    test('coincide por nombre, sin importar mayúsculas', () {
      expect(
        _producto(nombre: 'Coca Cola 600ml').coincideBusqueda('coca'),
        isTrue,
      );
    });

    test('coincide por código interno', () {
      expect(_producto(codigoInterno: 'P001').coincideBusqueda('p001'), isTrue);
    });

    test('coincide por código de barras', () {
      expect(
        _producto(codigoBarras: '7501234567890').coincideBusqueda('750123'),
        isTrue,
      );
    });

    test('no revienta cuando codigoBarras es null', () {
      expect(
        () => _producto(codigoBarras: null).coincideBusqueda('algo'),
        returnsNormally,
      );
      expect(_producto(codigoBarras: null).coincideBusqueda('algo'), isFalse);
    });

    test('query vacía o solo espacios coincide con todo', () {
      expect(_producto().coincideBusqueda(''), isTrue);
      expect(_producto().coincideBusqueda('   '), isTrue);
    });

    test('sin coincidencia devuelve false', () {
      expect(_producto(nombre: 'Coca Cola').coincideBusqueda('pepsi'), isFalse);
    });
  });

  group('coincideCodigoExacto', () {
    test('coincide exacto por código de barras', () {
      expect(
        _producto(
          codigoBarras: '7501234567890',
        ).coincideCodigoExacto('7501234567890'),
        isTrue,
      );
    });

    test('coincide exacto por código interno', () {
      expect(
        _producto(codigoInterno: 'P001').coincideCodigoExacto('P001'),
        isTrue,
      );
    });

    test('no es un match parcial — a diferencia de coincideBusqueda', () {
      expect(
        _producto(codigoBarras: '7501234567890').coincideCodigoExacto('750123'),
        isFalse,
      );
    });
  });
}
