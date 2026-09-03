import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/auth/application/auth_notifier.dart';
import 'package:market_pos/features/auth/data/sesion_usuario.dart';
import 'package:market_pos/features/auth/presentation/login_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _SinSesionAuthNotifier extends AuthNotifier {
  @override
  Future<SesionUsuario?> build() async => null;
}

Widget _app() {
  return ProviderScope(
    overrides: [
      authNotifierProvider.overrideWith(() => _SinSesionAuthNotifier()),
    ],
    child: const MaterialApp(home: LoginScreen()),
  );
}

void main() {
  testWidgets(
    'sin usuario recordado, el checkbox arranca desmarcado y el campo vacío',
    (tester) async {
      SharedPreferences.setMockInitialValues({});

      await tester.pumpWidget(_app());
      await tester.pumpAndSettle();

      expect(tester.widget<Checkbox>(find.byType(Checkbox)).value, isFalse);
      final campo = tester.widget<TextField>(find.byType(TextField).first);
      expect(campo.controller!.text, isEmpty);
    },
  );

  testWidgets('con usuario recordado, precarga el campo y marca el checkbox', (
    tester,
  ) async {
    SharedPreferences.setMockInitialValues({
      'inven365-usuario-recordado': 'cajero1',
    });

    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    expect(tester.widget<Checkbox>(find.byType(Checkbox)).value, isTrue);
    final campo = tester.widget<TextField>(find.byType(TextField).first);
    expect(campo.controller!.text, 'cajero1');
  });

  testWidgets('tocar el checkbox lo alterna', (tester) async {
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(_app());
    await tester.pumpAndSettle();

    await tester.tap(find.byType(Checkbox));
    await tester.pump();

    expect(tester.widget<Checkbox>(find.byType(Checkbox)).value, isTrue);
  });
}
