import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:market_pos/main.dart';

void main() {
  testWidgets('la app arranca en la pantalla de login', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const ProviderScope(child: MarketPosApp()));
    await tester.pumpAndSettle();

    expect(find.text('Bienvenido de nuevo'), findsOneWidget);
    expect(find.text('Usuario'), findsOneWidget);
    expect(find.text('Contraseña'), findsOneWidget);
    expect(find.text('Ingresar'), findsOneWidget);
  });
}
