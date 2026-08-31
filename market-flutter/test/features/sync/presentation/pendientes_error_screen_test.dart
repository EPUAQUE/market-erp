import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/sync/sync_engine.dart';
import 'package:market_pos/features/sync/presentation/pendientes_error_screen.dart';

ItemPendienteConError _item({
  TipoPendiente tipo = TipoPendiente.venta,
  int id = 1,
  String titulo = 'Venta — Q 8.50',
  String subtitulo = '1 línea(s) · efectivo',
  String mensajeError = 'STOCK_INSUFICIENTE: no hay existencia suficiente.',
}) {
  return ItemPendienteConError(
    tipo: tipo,
    id: id,
    titulo: titulo,
    subtitulo: subtitulo,
    mensajeError: mensajeError,
    creadaEn: DateTime(2026, 1, 1),
  );
}

Widget _app(List<ItemPendienteConError> items) {
  return ProviderScope(
    overrides: [pendientesConErrorProvider.overrideWith((ref) async => items)],
    child: const MaterialApp(home: PendientesErrorScreen()),
  );
}

void main() {
  testWidgets('lista vacía muestra el mensaje de "no hay pendientes"', (
    tester,
  ) async {
    await tester.pumpWidget(_app(const []));
    await tester.pumpAndSettle();

    expect(find.text('No hay ítems pendientes con error.'), findsOneWidget);
  });

  testWidgets('muestra una tarjeta por ítem con su mensaje de error', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app([
        _item(id: 1, titulo: 'Venta — Q 8.50'),
        _item(
          id: 2,
          tipo: TipoPendiente.cliente,
          titulo: 'Cliente nuevo — Juan Pérez',
          subtitulo: '',
          mensajeError: 'CLIENTE_DUPLICADO: ya existe un cliente con ese NIT.',
        ),
      ]),
    );
    await tester.pumpAndSettle();

    expect(find.text('Venta — Q 8.50'), findsOneWidget);
    expect(find.text('Cliente nuevo — Juan Pérez'), findsOneWidget);
    expect(
      find.text('STOCK_INSUFICIENTE: no hay existencia suficiente.'),
      findsOneWidget,
    );
    expect(
      find.text('CLIENTE_DUPLICADO: ya existe un cliente con ese NIT.'),
      findsOneWidget,
    );
    expect(find.text('REINTENTAR'), findsNWidgets(2));
    expect(find.text('DESCARTAR'), findsNWidgets(2));
  });

  testWidgets(
    'DESCARTAR pide confirmación antes de borrar — cancelar no descarta',
    (tester) async {
      await tester.pumpWidget(_app([_item()]));
      await tester.pumpAndSettle();

      await tester.tap(find.text('DESCARTAR'));
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Esta acción no se puede deshacer'),
        findsOneWidget,
      );

      await tester.tap(find.text('Cancelar'));
      await tester.pumpAndSettle();

      // El diálogo se cierra y el ítem sigue en la lista — cancelar no lo borra.
      expect(find.text('Venta — Q 8.50'), findsOneWidget);
    },
  );
}
