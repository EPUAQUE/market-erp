import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/util/decimal_input.dart';

void main() {
  group('parseDecimalInput', () {
    test('acepta punto como separador decimal', () {
      expect(parseDecimalInput('50.00'), Decimal.parse('50.00'));
    });

    test('acepta coma como separador decimal — bug reportado por el cliente: '
        'los botones de ingreso/egreso de caja no hacían nada al escribir '
        'un monto con coma (ej. teclado en es-GT)', () {
      expect(parseDecimalInput('50,00'), Decimal.parse('50.00'));
    });

    test('ignora espacios alrededor del valor', () {
      expect(parseDecimalInput('  50,00  '), Decimal.parse('50.00'));
    });

    test('un valor no numérico devuelve null', () {
      expect(parseDecimalInput('abc'), isNull);
    });

    test('cadena vacía devuelve null', () {
      expect(parseDecimalInput(''), isNull);
    });
  });
}
