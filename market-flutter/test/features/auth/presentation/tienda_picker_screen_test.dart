import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/features/auth/application/auth_notifier.dart';
import 'package:market_pos/features/auth/data/sesion_usuario.dart';
import 'package:market_pos/features/auth/presentation/tienda_picker_screen.dart';

class _SesionFijaAuthNotifier extends AuthNotifier {
  _SesionFijaAuthNotifier(this._sesion);

  final SesionUsuario? _sesion;

  @override
  Future<SesionUsuario?> build() async => _sesion;
}

Widget _app(SesionUsuario? sesion) {
  return ProviderScope(
    overrides: [
      authNotifierProvider.overrideWith(() => _SesionFijaAuthNotifier(sesion)),
    ],
    child: const MaterialApp(home: TiendaPickerScreen()),
  );
}

void main() {
  testWidgets('sin tiendas asignadas muestra el mensaje y el botón Salir', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(
        const SesionUsuario(
          username: 'vendedor',
          permisos: {},
          tiendaIds: {},
          alcanceGlobal: false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Este usuario no tiene ninguna tienda asignada.'),
      findsOneWidget,
    );
    expect(find.text('Salir'), findsOneWidget);
    expect(find.text('¿En qué tienda trabajas hoy?'), findsNothing);
  });

  testWidgets('con varias tiendas lista una fila por cada una', (tester) async {
    await tester.pumpWidget(
      _app(
        const SesionUsuario(
          username: 'vendedor',
          permisos: {},
          tiendaIds: {1, 2, 3},
          alcanceGlobal: false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('¿En qué tienda trabajas hoy?'), findsOneWidget);
    expect(find.text('Tienda #1'), findsOneWidget);
    expect(find.text('Tienda #2'), findsOneWidget);
    expect(find.text('Tienda #3'), findsOneWidget);
  });

  testWidgets(
    'Continuar empieza deshabilitado y se habilita al elegir una tienda',
    (tester) async {
      await tester.pumpWidget(
        _app(
          const SesionUsuario(
            username: 'vendedor',
            permisos: {},
            tiendaIds: {1, 2},
            alcanceGlobal: false,
          ),
        ),
      );
      await tester.pumpAndSettle();

      final continuar = find.widgetWithText(FilledButton, 'Continuar');
      expect(tester.widget<FilledButton>(continuar).onPressed, isNull);

      await tester.tap(find.text('Tienda #2'));
      await tester.pumpAndSettle();

      expect(tester.widget<FilledButton>(continuar).onPressed, isNotNull);
    },
  );
}
