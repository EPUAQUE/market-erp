import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/caja/data/caja.dart';
import 'package:market_pos/features/caja/domain/movimiento_caja_clasificado.dart';

MovimientoCaja _movimiento({
  required TipoMovimientoCaja tipo,
  required String concepto,
  required String monto,
}) {
  return MovimientoCaja(
    id: 1,
    fecha: DateTime(2026, 9, 6),
    tipo: tipo,
    concepto: concepto,
    monto: Decimal.parse(monto),
  );
}

void main() {
  group('ventasYCobrosEnEfectivo / totalVentasYCobrosEnEfectivo', () {
    test('una venta en efectivo cuenta completa', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #10 (EFECTIVO)',
          monto: '85.50',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), hasLength(1));
      expect(totalVentasYCobrosEnEfectivo(movimientos), Decimal.parse('85.50'));
    });

    test('venta con tarjeta o transferencia no cuenta, aunque el backend '
        'la trate como ingreso de caja', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #11 (TARJETA)',
          monto: '40.00',
        ),
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #12 (TRANSFERENCIA)',
          monto: '25.00',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), isEmpty);
      expect(totalVentasYCobrosEnEfectivo(movimientos), Decimal.zero);
    });

    test('una venta mixta solo aporta la porción en efectivo — el resto '
        '(tarjeta) llega como un MovimientoCaja separado', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #13 (EFECTIVO)',
          monto: '5.00',
        ),
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #13 (TARJETA)',
          monto: '3.50',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), hasLength(1));
      expect(totalVentasYCobrosEnEfectivo(movimientos), Decimal.parse('5.00'));
    });

    test('un cobro de cuenta por cobrar en efectivo cuenta como venta del '
        'día — también es efectivo real entrando a la gaveta', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Cobro cuenta por cobrar #9 (EFECTIVO)',
          monto: '30.00',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), hasLength(1));
      expect(totalVentasYCobrosEnEfectivo(movimientos), Decimal.parse('30.00'));
    });

    test('un cobro con tarjeta no cuenta', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Cobro cuenta por cobrar #9 (TARJETA)',
          monto: '30.00',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), isEmpty);
    });

    test(
      'un ingreso/egreso manual (concepto libre) nunca cuenta como venta',
      () {
        final movimientos = [
          _movimiento(
            tipo: TipoMovimientoCaja.ingreso,
            concepto: 'Cambio de caja chica',
            monto: '100.00',
          ),
          _movimiento(
            tipo: TipoMovimientoCaja.egreso,
            concepto: 'Pago proveedor',
            monto: '20.00',
          ),
        ];

        expect(ventasYCobrosEnEfectivo(movimientos), isEmpty);
      },
    );

    test('mezcla realista: suma solo lo que es efectivo real', () {
      final movimientos = [
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #1 (EFECTIVO)',
          monto: '50.00',
        ),
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Venta #2 (TARJETA)',
          monto: '99.00',
        ),
        _movimiento(
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Cobro cuenta por cobrar #5 (EFECTIVO)',
          monto: '30.00',
        ),
        _movimiento(
          tipo: TipoMovimientoCaja.egreso,
          concepto: 'Pago de luz',
          monto: '15.00',
        ),
      ];

      expect(ventasYCobrosEnEfectivo(movimientos), hasLength(2));
      expect(totalVentasYCobrosEnEfectivo(movimientos), Decimal.parse('80.00'));
    });
  });
}
