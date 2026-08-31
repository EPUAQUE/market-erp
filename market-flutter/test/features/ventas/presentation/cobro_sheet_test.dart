import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/ventas/presentation/cobro_sheet.dart';

Widget _app({Decimal? total}) {
  return ProviderScope(
    child: MaterialApp(
      home: Scaffold(
        body: CobroSheet(tiendaId: 1, total: total ?? Decimal.parse('20.00')),
      ),
    ),
  );
}

Finder _confirmarButton() => find.byWidgetPredicate(
  (widget) =>
      widget is FilledButton &&
      widget.child is Text &&
      ((widget.child as Text).data ?? '').startsWith('CONFIRMAR'),
);

void main() {
  testWidgets('arranca en Efectivo, confirmar deshabilitado sin monto', (
    tester,
  ) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    expect(find.text('Monto recibido'), findsOneWidget);
    expect(find.text('Cambio: —'), findsOneWidget);
    expect(tester.widget<FilledButton>(_confirmarButton()).onPressed, isNull);
  });

  testWidgets(
    'efectivo: monto insuficiente sigue deshabilitado, alcanzado calcula el cambio',
    (tester) async {
      await tester.pumpWidget(_app(total: Decimal.parse('20.00')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField).first, '15');
      await tester.pumpAndSettle();
      expect(tester.widget<FilledButton>(_confirmarButton()).onPressed, isNull);
      expect(find.text('Cambio: —'), findsOneWidget);

      await tester.enterText(find.byType(TextField).first, '25');
      await tester.pumpAndSettle();
      expect(
        tester.widget<FilledButton>(_confirmarButton()).onPressed,
        isNotNull,
      );
      expect(find.text('Cambio: Q 5'), findsOneWidget);
    },
  );

  testWidgets(
    'tarjeta/transferencia no piden monto y confirmar queda habilitado',
    (tester) async {
      await tester.pumpWidget(_app());
      await tester.pumpAndSettle();

      await tester.tap(find.text('Tarjeta'));
      await tester.pumpAndSettle();

      expect(find.text('Monto recibido'), findsNothing);
      expect(
        tester.widget<FilledButton>(_confirmarButton()).onPressed,
        isNotNull,
      );
    },
  );

  testWidgets('crédito exige elegir cliente antes de habilitar confirmar', (
    tester,
  ) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    await tester.tap(find.text('Crédito'));
    await tester.pumpAndSettle();

    expect(find.text('Selecciona un cliente'), findsOneWidget);
    expect(find.text('CONFIRMAR A CRÉDITO'), findsOneWidget);
    expect(tester.widget<FilledButton>(_confirmarButton()).onPressed, isNull);
  });

  testWidgets(
    'mixto solo habilita confirmar cuando la suma iguala el total exacto',
    (tester) async {
      await tester.pumpWidget(_app(total: Decimal.parse('8.50')));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Mixto'));
      await tester.pumpAndSettle();

      expect(find.text('Total ingresado: Q 0 de Q 8.5'), findsOneWidget);
      expect(tester.widget<FilledButton>(_confirmarButton()).onPressed, isNull);

      final campos = find.byType(TextField);
      await tester.enterText(campos.at(0), '5.00'); // Efectivo
      await tester.pumpAndSettle();
      expect(tester.widget<FilledButton>(_confirmarButton()).onPressed, isNull);

      await tester.enterText(campos.at(1), '3.50'); // Tarjeta
      await tester.pumpAndSettle();

      expect(find.text('Total ingresado: Q 8.5 de Q 8.5'), findsOneWidget);
      expect(
        tester.widget<FilledButton>(_confirmarButton()).onPressed,
        isNotNull,
      );
    },
  );
}
