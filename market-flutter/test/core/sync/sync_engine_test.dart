import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/core/db/local_store_provider.dart';
import 'package:market_pos/core/sync/sync_engine.dart';
import 'package:market_pos/features/ventas/application/checkout_notifier.dart';

import '../../support/venta_sync_fakes.dart';

/// Espera a que el drenado (disparado por el propio `ref.listen` de
/// `SyncEngineNotifier` al reconectar) termine, sin acceso directo a la
/// `Future` interna — sondea el resultado observable en vez de dormir un
/// tiempo fijo, ya que todo acá es async fake sin I/O real.
Future<void> esperarHasta(
  bool Function() condicion, {
  int maxIntentos = 200,
}) async {
  for (var i = 0; i < maxIntentos; i++) {
    if (condicion()) return;
    await Future<void>.delayed(Duration.zero);
  }
  fail('La condición no se cumplió a tiempo.');
}

void main() {
  group(
    'Reintento tras matar la app durante cada estado (Fase 2, PLAN_MEJORAS.md)',
    () {
      const clave = 'venta-kill-app';

      Future<
        ({
          ProviderContainer container,
          FakeVentaApiServidor api,
          FakeLocalStoreConVentaPendiente store,
        })
      >
      relanzarApp() async {
        final api = FakeVentaApiServidor();
        final store = FakeLocalStoreConVentaPendiente(
          ventaPendienteFake(correlationId: clave),
        );
        final container = ProviderContainer(
          overrides: [
            backendAlcanzableProvider.overrideWith((ref) => Stream.value(true)),
            localStoreProvider.overrideWith((ref) async => store),
            ventaApiProvider.overrideWithValue(api),
          ],
        );
        // Crea el SyncEngineNotifier YA (equivalente a que la app arranque y
        // registre su listener de conectividad) antes de que
        // backendAlcanzableProvider resuelva su primer valor — si se lee
        // después, el `ref.listen` interno de `build()` nunca alcanza a
        // registrarse a tiempo para la transición loading→true.
        container.listen(syncEngineProvider, (_, _) {});
        return (container: container, api: api, store: store);
      }

      test('el backend nunca vio la venta: el drenado la crea y completa '
          'normalmente', () async {
        final app = await relanzarApp();
        addTearDown(app.container.dispose);

        await esperarHasta(() => app.api.llamadasCompletar >= 1);
        await esperarHasta(() => app.store.resuelta);

        expect(app.api.ventas, hasLength(1));
        expect(app.api.ventas.single.estado, 'COMPLETADA');
        expect(app.store.marcoError, isFalse);
      });

      test('el kill pasó justo después de crear() (venta ya existía sin '
          'completar): el reintento no duplica la venta', () async {
        final app = await relanzarApp();
        addTearDown(app.container.dispose);
        app.api.sembrarEstadoPrevio(correlationId: clave, estado: 'BORRADOR');

        await esperarHasta(() => app.api.llamadasCompletar >= 1);
        await esperarHasta(() => app.store.resuelta);

        expect(app.api.ventas, hasLength(1));
        expect(app.api.ventas.single.estado, 'COMPLETADA');
        expect(app.store.marcoError, isFalse);
      });

      test(
        'el kill pasó justo después de completar() (venta ya estaba '
        'COMPLETADA): el reintento la reconoce en vez de marcarla con error',
        () async {
          final app = await relanzarApp();
          addTearDown(app.container.dispose);
          app.api.sembrarEstadoPrevio(
            correlationId: clave,
            estado: 'COMPLETADA',
          );

          await esperarHasta(() => app.store.resuelta);

          expect(app.api.ventas, hasLength(1));
          expect(app.api.ventas.single.estado, 'COMPLETADA');
          expect(app.api.llamadasCompletar, greaterThanOrEqualTo(1));
          expect(app.store.marcoError, isFalse);
        },
      );
    },
  );
}
