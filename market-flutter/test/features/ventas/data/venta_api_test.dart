import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/ventas/data/venta.dart';
import 'package:market_pos/features/ventas/data/venta_api.dart';

void main() {
  group('metodoPagoToJson', () {
    test('serializa cada valor en mayúsculas', () {
      expect(metodoPagoToJson(MetodoPago.efectivo), 'EFECTIVO');
      expect(metodoPagoToJson(MetodoPago.tarjeta), 'TARJETA');
      expect(metodoPagoToJson(MetodoPago.transferencia), 'TRANSFERENCIA');
      expect(metodoPagoToJson(MetodoPago.credito), 'CREDITO');
      expect(metodoPagoToJson(MetodoPago.mixto), 'MIXTO');
    });
  });

  group('metodoPagoFromJson', () {
    test('reconoce mayúsculas (formato del backend)', () {
      expect(metodoPagoFromJson('TARJETA'), MetodoPago.tarjeta);
    });

    test('reconoce minúsculas (formato guardado en LocalStore)', () {
      expect(metodoPagoFromJson('tarjeta'), MetodoPago.tarjeta);
    });

    test('es insensible a mayúsculas mixtas', () {
      expect(metodoPagoFromJson('CrEdItO'), MetodoPago.credito);
    });

    test('cae a efectivo con un valor desconocido o null', () {
      expect(metodoPagoFromJson('algo-invalido'), MetodoPago.efectivo);
      expect(metodoPagoFromJson(null), MetodoPago.efectivo);
    });
  });

  group('Venta.fromJson', () {
    test('parsea todos los campos, total como Decimal exacto', () {
      final venta = Venta.fromJson({
        'id': 42,
        'clienteId': 7,
        'estado': 'COMPLETADA',
        'total': '125.50',
      });
      expect(venta.id, 42);
      expect(venta.clienteId, 7);
      expect(venta.estado, 'COMPLETADA');
      expect(venta.total, Decimal.parse('125.50'));
    });
  });

  group('CuentaPorCobrar.fromJson', () {
    Map<String, dynamic> json({
      String estado = 'PENDIENTE',
      String fechaVencimiento = '2026-01-01T00:00:00Z',
    }) => {
      'id': 1,
      'ventaId': 10,
      'clienteId': 3,
      'saldoPendiente': '8.50',
      'estado': estado,
      'fechaVencimiento': fechaVencimiento,
    };

    test('parsea saldoPendiente como Decimal exacto', () {
      final cuenta = CuentaPorCobrar.fromJson(json());
      expect(cuenta.saldoPendiente, Decimal.parse('8.50'));
    });

    test('pendiente es true solo con estado PENDIENTE', () {
      expect(
        CuentaPorCobrar.fromJson(json(estado: 'PENDIENTE')).pendiente,
        isTrue,
      );
      expect(
        CuentaPorCobrar.fromJson(json(estado: 'COBRADA')).pendiente,
        isFalse,
      );
      expect(
        CuentaPorCobrar.fromJson(json(estado: 'ANULADA')).pendiente,
        isFalse,
      );
    });

    test('vencida es true solo si pendiente y ya pasó la fecha', () {
      final vencida = CuentaPorCobrar.fromJson(
        json(estado: 'PENDIENTE', fechaVencimiento: '2000-01-01T00:00:00Z'),
      );
      expect(vencida.vencida, isTrue);

      final futura = CuentaPorCobrar.fromJson(
        json(estado: 'PENDIENTE', fechaVencimiento: '2100-01-01T00:00:00Z'),
      );
      expect(futura.vencida, isFalse);

      final cobradaVencida = CuentaPorCobrar.fromJson(
        json(estado: 'COBRADA', fechaVencimiento: '2000-01-01T00:00:00Z'),
      );
      expect(cobradaVencida.vencida, isFalse);
    });
  });
}
