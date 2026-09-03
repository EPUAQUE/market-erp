import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/auth/presentation/reset_password_screen.dart';

Widget _app() {
  return const ProviderScope(child: MaterialApp(home: ResetPasswordScreen()));
}

void main() {
  testWidgets('código vacío muestra su error de validación', (tester) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    await tester.tap(
      find.widgetWithText(FilledButton, 'Guardar nueva contraseña'),
    );
    await tester.pump();

    expect(
      find.text('Pega el código que recibiste por correo.'),
      findsOneWidget,
    );
  });

  testWidgets('contraseña corta muestra su error de validación', (
    tester,
  ) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextField, 'Código recibido por correo'),
      'un-codigo-cualquiera',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Nueva contraseña'),
      'corta',
    );
    await tester.tap(
      find.widgetWithText(FilledButton, 'Guardar nueva contraseña'),
    );
    await tester.pump();

    expect(
      find.text('La contraseña debe tener al menos 12 caracteres.'),
      findsOneWidget,
    );
  });

  testWidgets('contraseñas que no coinciden muestran su error de validación', (
    tester,
  ) async {
    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextField, 'Código recibido por correo'),
      'un-codigo-cualquiera',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Nueva contraseña'),
      'contrasena-larga-1',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Confirmar contraseña'),
      'otra-contrasena-2',
    );
    await tester.tap(
      find.widgetWithText(FilledButton, 'Guardar nueva contraseña'),
    );
    await tester.pump();

    expect(find.text('Las contraseñas no coinciden.'), findsOneWidget);
  });
}
