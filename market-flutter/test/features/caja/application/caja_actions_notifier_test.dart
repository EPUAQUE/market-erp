import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/core/db/local_store_provider.dart';
import 'package:market_pos/core/db/local_store_web.dart';
import 'package:market_pos/core/network/api_client.dart';
import 'package:market_pos/features/caja/application/caja_provider.dart';
import 'package:market_pos/features/caja/data/caja.dart';
import 'package:market_pos/features/caja/data/caja_api.dart';
import 'package:market_pos/features/caja/data/movimiento_caja_pendiente_local.dart';

/// Antes de esta fase, `CajaApi`/`CajaActionsNotifier` nunca mandaban
/// `correlationId` pese a que el backend lo soporta desde Fase 2
/// (`AbrirCajaRequest`/`RegistrarMovimientoCajaRequest`/`CerrarCajaRequest`)
/// — un movimiento cuya respuesta se pierde, reintentado automáticamente por
/// `SyncEngine` al reconectar, se procesaba como un ingreso/egreso NUEVO en
/// vez de idempotente. Este fake registra qué clave recibió cada llamada
/// para poder probar que la plomería llega de verdad hasta la red.
class FakeCajaApiServidor extends CajaApi {
  FakeCajaApiServidor() : super(ApiClient.instance);

  int llamadasAbrir = 0;
  int llamadasRegistrarMovimiento = 0;
  int llamadasCerrar = 0;
  final List<String?> correlationIdsRecibidosEnMovimiento = [];
  String? correlationIdRecibidoEnAbrir;
  String? correlationIdRecibidoEnCerrar;

  CajaSesion _sesionFake({String estado = 'ABIERTA'}) => CajaSesion(
    id: 1,
    montoInicial: Decimal.parse('100'),
    montoFinalContado: null,
    saldoEsperado: Decimal.parse('100'),
    estado: estado,
    movimientos: const [],
  );

  @override
  Future<CajaSesion> abrir({
    required int tiendaId,
    required Decimal montoInicial,
    String? correlationId,
  }) async {
    llamadasAbrir++;
    correlationIdRecibidoEnAbrir = correlationId;
    return _sesionFake();
  }

  @override
  Future<CajaSesion> registrarMovimiento({
    required int tiendaId,
    required TipoMovimientoCaja tipo,
    required String concepto,
    required Decimal monto,
    String? correlationId,
  }) async {
    llamadasRegistrarMovimiento++;
    correlationIdsRecibidosEnMovimiento.add(correlationId);
    return _sesionFake();
  }

  @override
  Future<CajaSesion> cerrar({
    required int tiendaId,
    required Decimal montoFinalContado,
    String? correlationId,
  }) async {
    llamadasCerrar++;
    correlationIdRecibidoEnCerrar = correlationId;
    return _sesionFake(estado: 'CERRADA');
  }
}

/// Cola offline de movimientos con almacenamiento "disponible" — mismo
/// espíritu que los fakes de `venta_sync_fakes.dart`, pero acá lo que
/// importa es qué `correlationId` quedó grabado en cada ítem encolado.
class FakeLocalStoreMovimientosOffline extends WebLocalStore {
  final List<NuevoMovimientoCajaPendiente> movimientosEncolados = [];

  @override
  bool get disponible => true;

  @override
  Future<void> encolarMovimientoCajaPendiente(
    NuevoMovimientoCajaPendiente movimiento,
  ) async {
    movimientosEncolados.add(movimiento);
  }
}

void main() {
  group('Idempotencia de caja (Fase 2, PLAN_MEJORAS.md)', () {
    test('Wi-Fi activo con backend caído: el movimiento se encola con un '
        'correlationId propio, nunca llama a la API online', () async {
      final api = FakeCajaApiServidor();
      final store = FakeLocalStoreMovimientosOffline();
      final container = ProviderContainer(
        overrides: [
          backendAlcanzableProvider.overrideWith((ref) => Stream.value(false)),
          localStoreProvider.overrideWith((ref) async => store),
          cajaApiProvider.overrideWithValue(api),
        ],
      );
      addTearDown(container.dispose);
      container.listen(backendAlcanzableProvider, (_, _) {});
      await container.read(backendAlcanzableProvider.future);

      final exito = await container
          .read(cajaActionsProvider.notifier)
          .registrarMovimiento(
            tiendaId: 1,
            tipo: TipoMovimientoCaja.ingreso,
            concepto: 'Venta suelta',
            monto: Decimal.parse('50'),
          );

      expect(exito, isTrue);
      expect(api.llamadasRegistrarMovimiento, 0);
      expect(store.movimientosEncolados, hasLength(1));
      expect(store.movimientosEncolados.single.correlationId, isNotEmpty);
      expect(container.read(cajaActionsProvider).error, isNull);
    });

    test(
      'dos envíos distintos (dos diálogos separados) generan cada uno su '
      'propia clave — no hay "reintento sobre la misma clave" para caja',
      () async {
        final api = FakeCajaApiServidor();
        final store = FakeLocalStoreMovimientosOffline();
        final container = ProviderContainer(
          overrides: [
            backendAlcanzableProvider.overrideWith(
              (ref) => Stream.value(false),
            ),
            localStoreProvider.overrideWith((ref) async => store),
            cajaApiProvider.overrideWithValue(api),
          ],
        );
        addTearDown(container.dispose);
        container.listen(backendAlcanzableProvider, (_, _) {});
        await container.read(backendAlcanzableProvider.future);

        final notifier = container.read(cajaActionsProvider.notifier);
        await notifier.registrarMovimiento(
          tiendaId: 1,
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Primer ingreso',
          monto: Decimal.parse('20'),
        );
        await notifier.registrarMovimiento(
          tiendaId: 1,
          tipo: TipoMovimientoCaja.ingreso,
          concepto: 'Segundo ingreso',
          monto: Decimal.parse('30'),
        );

        expect(store.movimientosEncolados, hasLength(2));
        expect(
          store.movimientosEncolados[0].correlationId,
          isNot(store.movimientosEncolados[1].correlationId),
        );
      },
    );

    test('online: abrir/registrarMovimiento/cerrar mandan un correlationId '
        'propio a la API — antes no mandaban ninguno', () async {
      final api = FakeCajaApiServidor();
      final container = ProviderContainer(
        overrides: [
          backendAlcanzableProvider.overrideWith((ref) => Stream.value(true)),
          cajaApiProvider.overrideWithValue(api),
        ],
      );
      addTearDown(container.dispose);
      container.listen(backendAlcanzableProvider, (_, _) {});
      await container.read(backendAlcanzableProvider.future);

      final notifier = container.read(cajaActionsProvider.notifier);
      await notifier.abrir(tiendaId: 1, montoInicial: Decimal.parse('100'));
      await notifier.registrarMovimiento(
        tiendaId: 1,
        tipo: TipoMovimientoCaja.egreso,
        concepto: 'Pago proveedor',
        monto: Decimal.parse('15'),
      );
      await notifier.cerrar(
        tiendaId: 1,
        montoFinalContado: Decimal.parse('85'),
      );

      expect(api.llamadasAbrir, 1);
      expect(api.correlationIdRecibidoEnAbrir, isNotNull);
      expect(api.correlationIdRecibidoEnAbrir, isNotEmpty);

      expect(api.llamadasRegistrarMovimiento, 1);
      expect(api.correlationIdsRecibidosEnMovimiento.single, isNotNull);
      expect(api.correlationIdsRecibidosEnMovimiento.single, isNotEmpty);

      expect(api.llamadasCerrar, 1);
      expect(api.correlationIdRecibidoEnCerrar, isNotNull);
      expect(api.correlationIdRecibidoEnCerrar, isNotEmpty);

      // Las tres claves son independientes entre sí — nada las deriva
      // unas de otras.
      expect({
        api.correlationIdRecibidoEnAbrir,
        api.correlationIdsRecibidosEnMovimiento.single,
        api.correlationIdRecibidoEnCerrar,
      }, hasLength(3));
    });
  });
}
