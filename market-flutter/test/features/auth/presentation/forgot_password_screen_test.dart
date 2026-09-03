import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/auth/presentation/forgot_password_screen.dart';

Widget _app() {
  return const ProviderScope(child: MaterialApp(home: ForgotPasswordScreen()));
}

void main() {
  testWidgets(
    'usuario vacío muestra el error de validación sin llamar al backend',
    (tester) async {
      await tester.pumpWidget(_app());
      await tester.pumpAndSettle();

      await tester.tap(find.widgetWithText(FilledButton, 'Enviar enlace'));
      await tester.pump();

      expect(find.text('Ingrese su usuario.'), findsOneWidget);
    },
  );

  testWidgets('muestra el formulario inicial con el título esperado', (
    tester,
  ) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    expect(find.text('¿Olvidaste tu contraseña?'), findsOneWidget);
    expect(find.text('Revisa tu correo'), findsNothing);
  });
}
