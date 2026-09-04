import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/core/connectivity/connectivity_provider.dart';
import 'package:market_pos/core/db/local_store_provider.dart';
import 'package:market_pos/core/sync/sync_engine.dart';
import 'package:market_pos/features/ventas/application/checkout_notifier.dart';

import '../../support/venta_sync_fakes.dart';
import 'sync_engine_test.dart' show esperarHasta;

/// "Cambio entre Wi-Fi y datos durante sincronización" (Fase 2,
/// PLAN_MEJORAS.md): lo único que le importa a `SyncEngineNotifier` de ese
/// cambio es el corte transitorio que produce mientras el dispositivo
/// conmuta de una interfaz a otra — `backendAlcanzableProvider` no
/// distingue Wi-Fi de datos móviles, solo "el backend responde o no". Se
/// simula manejando manualmente el `Stream` que ese provider expondría:
/// conectado → un fallo de red a mitad del drenado (la conmutación en sí) →
/// reconectado, y se confirma que la venta termina sincronizada exactamente
/// una vez, sin quedar marcada con error por ese corte transitorio.
void main() {
  test('un corte de red a mitad del drenado (ej. Wi-Fi→datos) no marca la '
      'venta con error ni la duplica al reconectar', () async {
    const clave = 'venta-cambio-red';
    final api = FakeVentaApiServidor()..fallosDeRedRestantesEnCrear = 1;
    final store = FakeLocalStoreConVentaPendiente(
      ventaPendienteFake(correlationId: clave),
    );
    final conexion = StreamController<bool>();
    addTearDown(conexion.close);

    final container = ProviderContainer(
      overrides: [
        backendAlcanzableProvider.overrideWith((ref) => conexion.stream),
        localStoreProvider.overrideWith((ref) async => store),
        ventaApiProvider.overrideWithValue(api),
      ],
    );
    addTearDown(container.dispose);
    // Registra el SyncEngineNotifier (y su `ref.listen` interno) antes de
    // emitir el primer valor de conectividad — mismo motivo que en
    // sync_engine_test.dart.
    container.listen(syncEngineProvider, (_, _) {});

    // 1) Conectado: el drenado arranca, pero el primer crear() falla por
    // red (la conmutación de interfaz en curso) — la venta debe quedarse
    // en la cola, sin marcarse con error, esperando la reconexión.
    conexion.add(true);
    await esperarHasta(() => api.llamadasCrear >= 1);
    await esperarHasta(
      () => container.read(syncEngineProvider) == EstadoConexion.conectado,
    );
    expect(store.resuelta, isFalse);
    expect(store.marcoError, isFalse);
    expect(api.ventas, isEmpty);

    // 2) Cae del todo (la propia conmutación de interfaz) — el motor debe
    // reflejarlo, aunque en este punto ya no había nada en vuelo.
    conexion.add(false);
    await esperarHasta(
      () => container.read(syncEngineProvider) == EstadoConexion.sinConexion,
    );

    // 3) Reconecta ya en la otra interfaz (datos): el drenado se repite
    // desde cero para la misma venta — crear() esta vez sí llega, y todo
    // el flujo se completa con una sola venta, sin duplicar el intento
    // fallido anterior.
    conexion.add(true);
    await esperarHasta(() => store.resuelta);

    expect(api.ventas, hasLength(1));
    expect(api.ventas.single.estado, 'COMPLETADA');
    expect(store.marcoError, isFalse);
  });
}
