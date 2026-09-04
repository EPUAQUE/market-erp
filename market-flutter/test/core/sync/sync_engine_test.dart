import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/core/db/local_store_provider.dart';
import 'package:market_pos/core/db/local_store_web.dart';
import 'package:market_pos/core/network/api_client.dart';
import 'package:market_pos/core/network/api_exception.dart';
import 'package:market_pos/core/sync/sync_engine.dart';
import 'package:market_pos/features/ventas/application/checkout_notifier.dart';
import 'package:market_pos/features/ventas/data/venta.dart';
import 'package:market_pos/features/ventas/data/venta_api.dart';
import 'package:market_pos/features/ventas/data/venta_pendiente_local.dart';
import 'package:market_pos/features/ventas/domain/carrito.dart';

/// Fila server-side, tal como quedaría el backend real después de que la
/// app se mató a mitad de sincronizar — lo que un `SyncEngineNotifier`
/// recién creado (equivalente a relanzar la app) se encuentra al reintentar
/// desde cero, sin memoria de qué llamada alcanzó a salir antes del kill.
class _VentaEnServidor {
  _VentaEnServidor(this.id, this.correlationId, this.estado);
  final int id;
  final String correlationId;
  String estado;
}

class _VentaApiConEstadoPrevio extends VentaApi {
  _VentaApiConEstadoPrevio() : super(ApiClient.instance);

  final List<_VentaEnServidor> _ventas = [];
  int _siguienteId = 1;
  int llamadasCrear = 0;
  int llamadasCompletar = 0;

  /// Precarga el estado en el que había quedado el backend ANTES del kill,
  /// para esta clave de idempotencia — sin esto, el escenario es "el
  /// backend nunca llegó a ver la venta".
  void sembrarEstadoPrevio({
    required String correlationId,
    required String estado,
  }) {
    _ventas.add(_VentaEnServidor(_siguienteId++, correlationId, estado));
  }

  _VentaEnServidor? _porCorrelationId(String? id) {
    for (final v in _ventas) {
      if (v.correlationId == id) return v;
    }
    return null;
  }

  _VentaEnServidor _porId(int id) => _ventas.firstWhere((v) => v.id == id);

  Venta _comoVenta(_VentaEnServidor v) => Venta(
    id: v.id,
    clienteId: 42,
    estado: v.estado,
    total: Decimal.parse('8.50'),
  );

  @override
  Future<Venta> crear({
    required int tiendaId,
    required int clienteId,
    required List<LineaCarrito> lineas,
    required MetodoPago metodoPago,
    String? correlationId,
  }) async {
    llamadasCrear++;
    final existente = _porCorrelationId(correlationId);
    final venta =
        existente ??
        _VentaEnServidor(_siguienteId++, correlationId!, 'BORRADOR');
    if (existente == null) _ventas.add(venta);
    return _comoVenta(venta);
  }

  @override
  Future<Venta> completar({
    required int tiendaId,
    required int ventaId,
    Map<MetodoPago, Decimal>? pagosInmediatos,
  }) async {
    llamadasCompletar++;
    final venta = _porId(ventaId);
    if (venta.estado == 'COMPLETADA') {
      throw ApiException(
        message:
            'La operación no es válida para una venta en estado COMPLETADA',
        status: 409,
        code: 'ESTADO_VENTA_INVALIDO',
      );
    }
    venta.estado = 'COMPLETADA';
    return _comoVenta(venta);
  }

  @override
  Future<Venta> obtener({required int tiendaId, required int ventaId}) async =>
      _comoVenta(_porId(ventaId));
}

/// Cola offline con una única venta pendiente precargada — simula lo que
/// quedó persistido en disco (Isar en la app real) al momento del kill.
class _LocalStoreConVentaPendiente extends WebLocalStore {
  _LocalStoreConVentaPendiente(this._venta);

  VentaPendienteLocal? _venta;
  bool marcoError = false;
  String? mensajeErrorRecibido;

  @override
  bool get disponible => true;

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientes() async =>
      _venta == null ? const [] : [_venta!];

  @override
  Future<void> marcarVentaPendienteConError(int id, String mensaje) async {
    marcoError = true;
    mensajeErrorRecibido = mensaje;
  }

  @override
  Future<void> eliminarVentaPendiente(int id) async {
    _venta = null;
  }

  @override
  Future<int> contarVentasPendientes() async => _venta == null ? 0 : 1;
}

VentaPendienteLocal _ventaPendiente({required String correlationId}) {
  return VentaPendienteLocal(
    id: 1,
    correlationId: correlationId,
    tiendaId: 1,
    clienteId: 42,
    clientePendienteLocalId: null,
    lineas: [
      LineaCarrito(
        productoId: 1,
        nombre: 'Producto 1',
        precioUnitario: Decimal.parse('8.50'),
        cantidad: 1,
      ),
    ],
    metodoPago: MetodoPago.efectivo.name,
    montoACobrar: Decimal.parse('8.50'),
    creadaEn: DateTime(2026, 1, 1),
    mensajeError: null,
  );
}

/// Espera a que el drenado (disparado por el propio `ref.listen` de
/// `SyncEngineNotifier` al reconectar) termine, sin acceso directo a la
/// `Future` interna — sondea el resultado observable en vez de dormir un
/// tiempo fijo, ya que todo acá es async fake sin I/O real.
Future<void> _esperarHasta(
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
          _VentaApiConEstadoPrevio api,
          _LocalStoreConVentaPendiente store,
        })
      >
      relanzarApp() async {
        final api = _VentaApiConEstadoPrevio();
        final store = _LocalStoreConVentaPendiente(
          _ventaPendiente(correlationId: clave),
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

        await _esperarHasta(() => app.api.llamadasCompletar >= 1);
        await _esperarHasta(() => app.store._venta == null);

        expect(app.api._ventas, hasLength(1));
        expect(app.api._ventas.single.estado, 'COMPLETADA');
        expect(app.store.marcoError, isFalse);
      });

      test('el kill pasó justo después de crear() (venta ya existía sin '
          'completar): el reintento no duplica la venta', () async {
        final app = await relanzarApp();
        addTearDown(app.container.dispose);
        app.api.sembrarEstadoPrevio(correlationId: clave, estado: 'BORRADOR');

        await _esperarHasta(() => app.api.llamadasCompletar >= 1);
        await _esperarHasta(() => app.store._venta == null);

        expect(app.api._ventas, hasLength(1));
        expect(app.api._ventas.single.estado, 'COMPLETADA');
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

          await _esperarHasta(() => app.store._venta == null);

          expect(app.api._ventas, hasLength(1));
          expect(app.api._ventas.single.estado, 'COMPLETADA');
          expect(app.api.llamadasCompletar, greaterThanOrEqualTo(1));
          expect(app.store.marcoError, isFalse);
        },
      );
    },
  );
}
