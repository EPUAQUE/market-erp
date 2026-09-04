import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/core/db/local_store_provider.dart';
import 'package:market_pos/core/db/local_store_web.dart';
import 'package:market_pos/core/network/api_client.dart';
import 'package:market_pos/core/network/api_exception.dart';
import 'package:market_pos/features/productos/data/producto_catalogo.dart';
import 'package:market_pos/features/ventas/application/carrito_notifier.dart';
import 'package:market_pos/features/ventas/application/checkout_notifier.dart';
import 'package:market_pos/features/ventas/domain/carrito.dart';
import 'package:market_pos/features/ventas/data/venta.dart';
import 'package:market_pos/features/ventas/data/venta_api.dart';
import 'package:market_pos/features/ventas/data/venta_pendiente_local.dart';

/// Simula "hay interfaz de red pero el backend no responde" — exactamente lo
/// que `backendAlcanzableProvider` colapsa a `false` (ver
/// `backend_reachability_provider.dart`, PLAN_MEJORAS.md Fase 2).
Stream<bool> _redSinBackend() => Stream.value(false);

/// Nunca debería llegar a llamarse mientras el backend esté marcado
/// inalcanzable — si algo la invoca, el test debe fallar de forma explícita
/// en vez de silenciosamente intentar una request de red real.
class _VentaApiNuncaLlamada extends VentaApi {
  _VentaApiNuncaLlamada() : super(ApiClient.instance);

  bool llamada = false;

  @override
  Future<Venta> crear({
    required int tiendaId,
    required int clienteId,
    required List<LineaCarrito> lineas,
    required MetodoPago metodoPago,
    String? correlationId,
  }) async {
    llamada = true;
    throw StateError(
      'No debe llamarse la API online con el backend inalcanzable.',
    );
  }

  @override
  Future<Venta> completar({
    required int tiendaId,
    required int ventaId,
    Map<MetodoPago, Decimal>? pagosInmediatos,
  }) async {
    llamada = true;
    throw StateError(
      'No debe llamarse la API online con el backend inalcanzable.',
    );
  }
}

/// Mismo espíritu que `WebLocalStore` (no-op) pero con almacenamiento local
/// "disponible" y con `encolarVentaPendiente` grabando lo que se encoló, para
/// poder verificar la clave de idempotencia sin tocar Isar de verdad.
class _LocalStoreOfflineDisponible extends WebLocalStore {
  final List<NuevaVentaPendiente> ventasEncoladas = [];

  @override
  bool get disponible => true;

  @override
  Future<void> encolarVentaPendiente(NuevaVentaPendiente venta) async {
    ventasEncoladas.add(venta);
  }
}

/// Estado server-side de una venta, tal como lo vería el backend real —
/// permite simular que un `crear()`/`completar()` de un intento anterior sí
/// tuvo éxito ahí aunque la respuesta nunca haya llegado al cliente.
class _VentaEnServidor {
  _VentaEnServidor(this.id, this.correlationId);
  final int id;
  final String correlationId;
  String estado = 'BORRADOR';
}

/// Simula el backend real lo suficiente para probar la recuperación ante una
/// respuesta perdida: `crear()` es idempotente por `correlationId` (nunca
/// crea una segunda venta para la misma clave) y `completar()` sobre una
/// venta ya `COMPLETADA` responde `409 ESTADO_VENTA_INVALIDO`, igual que el
/// backend real (ver CLAUDE.md, "Sync retry bug").
class _VentaApiServidorSimulado extends VentaApi {
  _VentaApiServidorSimulado() : super(ApiClient.instance);

  final List<_VentaEnServidor> _ventas = [];
  int _siguienteId = 1;
  int llamadasObtener = 0;

  /// Si es `true`, la próxima vez que `crear()`/`completar()` procese algo
  /// de verdad server-side lanza un error de red al cliente en vez de
  /// devolver la respuesta — "el POST llegó y se aplicó, pero la respuesta
  /// se perdió en el camino de vuelta". Se apaga solo tras dispararse una
  /// vez, para que un reintento posterior sí reciba respuesta normal.
  bool perderRespuestaDelProximoCrear = false;
  bool perderRespuestaDelProximoCompletar = false;

  _VentaEnServidor? _buscar(int id) {
    for (final v in _ventas) {
      if (v.id == id) return v;
    }
    return null;
  }

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
    _VentaEnServidor? existente;
    for (final v in _ventas) {
      if (v.correlationId == correlationId) existente = v;
    }
    final esNueva = existente == null;
    final venta = existente ?? _VentaEnServidor(_siguienteId++, correlationId!);
    if (esNueva) _ventas.add(venta);
    if (esNueva && perderRespuestaDelProximoCrear) {
      perderRespuestaDelProximoCrear = false;
      throw ApiException(
        message: 'No se pudo conectar con el servidor.',
        isNetworkError: true,
      );
    }
    return _comoVenta(venta);
  }

  @override
  Future<Venta> completar({
    required int tiendaId,
    required int ventaId,
    Map<MetodoPago, Decimal>? pagosInmediatos,
  }) async {
    final venta = _buscar(ventaId)!;
    if (venta.estado == 'COMPLETADA') {
      throw ApiException(
        message:
            'La operación no es válida para una venta en estado COMPLETADA',
        status: 409,
        code: 'ESTADO_VENTA_INVALIDO',
      );
    }
    venta.estado = 'COMPLETADA';
    if (perderRespuestaDelProximoCompletar) {
      perderRespuestaDelProximoCompletar = false;
      throw ApiException(
        message: 'No se pudo conectar con el servidor.',
        isNetworkError: true,
      );
    }
    return _comoVenta(venta);
  }

  @override
  Future<Venta> obtener({required int tiendaId, required int ventaId}) async {
    llamadasObtener++;
    return _comoVenta(_buscar(ventaId)!);
  }
}

ProductoCatalogo _producto() => ProductoCatalogo(
  productoId: 1,
  codigoInterno: 'P001',
  codigoBarras: null,
  nombre: 'Producto 1',
  descripcionCorta: null,
  imagenUrl: null,
  precioVenta: Decimal.parse('8.50'),
  existenciaActual: Decimal.parse('10'),
  permitirVenta: true,
  categoriaId: 1,
);

void main() {
  group('Wi-Fi activo con backend caído (Fase 2, PLAN_MEJORAS.md)', () {
    test('con almacenamiento local disponible: la venta se encola con el '
        'correlationId, nunca llama a la API online y no se pierde', () async {
      final ventaApi = _VentaApiNuncaLlamada();
      final store = _LocalStoreOfflineDisponible();
      final container = ProviderContainer(
        overrides: [
          backendAlcanzableProvider.overrideWith((ref) => _redSinBackend()),
          localStoreProvider.overrideWith((ref) async => store),
          ventaApiProvider.overrideWithValue(ventaApi),
        ],
      );
      addTearDown(container.dispose);
      // Sin un listener, un StreamProvider sin `keepAlive` puede
      // descartarse antes de que `.future` resuelva su primer valor.
      container.listen(backendAlcanzableProvider, (_, _) {});

      // Deja que backendAlcanzableProvider resuelva su primer valor antes
      // de confirmar — si se lee antes de resolver, CheckoutNotifier asume
      // `true` por defecto (ver checkout_notifier.dart) y este test no
      // estaría probando el escenario real.
      await container.read(backendAlcanzableProvider.future);
      expect(container.read(backendAlcanzableProvider).value, isFalse);

      container.read(carritoProvider.notifier).agregarProducto(_producto());

      await container
          .read(checkoutProvider.notifier)
          .confirmar(
            tiendaId: 1,
            metodo: MetodoPago.efectivo,
            correlationId: 'venta-offline-1',
          );

      final estado = container.read(checkoutProvider);
      expect(estado.error, isNull);
      expect(estado.ventaCompletada, isTrue);
      expect(ventaApi.llamada, isFalse);
      expect(store.ventasEncoladas, hasLength(1));
      expect(store.ventasEncoladas.single.correlationId, 'venta-offline-1');
      expect(container.read(carritoProvider).estaVacio, isTrue);
    });

    test(
      'reintento manual con la misma clave sigue sin tocar la API online '
      'y conserva el mismo correlationId (no genera una identidad nueva)',
      () async {
        final ventaApi = _VentaApiNuncaLlamada();
        final store = _LocalStoreOfflineDisponible();
        final container = ProviderContainer(
          overrides: [
            backendAlcanzableProvider.overrideWith((ref) => _redSinBackend()),
            localStoreProvider.overrideWith((ref) async => store),
            ventaApiProvider.overrideWithValue(ventaApi),
          ],
        );
        addTearDown(container.dispose);
        container.listen(backendAlcanzableProvider, (_, _) {});
        await container.read(backendAlcanzableProvider.future);

        const mismaClave = 'venta-offline-retry';
        for (var intento = 0; intento < 2; intento++) {
          container.read(carritoProvider.notifier).agregarProducto(_producto());
          await container
              .read(checkoutProvider.notifier)
              .confirmar(
                tiendaId: 1,
                metodo: MetodoPago.efectivo,
                correlationId: mismaClave,
              );
        }

        expect(ventaApi.llamada, isFalse);
        expect(store.ventasEncoladas, hasLength(2));
        expect(
          store.ventasEncoladas.map((v) => v.correlationId),
          everyElement(mismaClave),
        );
      },
    );

    test('sin almacenamiento local disponible (web): falla explícito, nunca '
        'finge estar en línea ni pierde la venta en silencio', () async {
      final ventaApi = _VentaApiNuncaLlamada();
      final container = ProviderContainer(
        overrides: [
          backendAlcanzableProvider.overrideWith((ref) => _redSinBackend()),
          localStoreProvider.overrideWith((ref) async => const WebLocalStore()),
          ventaApiProvider.overrideWithValue(ventaApi),
        ],
      );
      addTearDown(container.dispose);
      container.listen(backendAlcanzableProvider, (_, _) {});
      await container.read(backendAlcanzableProvider.future);

      container.read(carritoProvider.notifier).agregarProducto(_producto());

      await container
          .read(checkoutProvider.notifier)
          .confirmar(
            tiendaId: 1,
            metodo: MetodoPago.efectivo,
            correlationId: 'venta-sin-storage',
          );

      final estado = container.read(checkoutProvider);
      expect(estado.error, isNotNull);
      expect(estado.ventaCompletada, isFalse);
      expect(ventaApi.llamada, isFalse);
      // El carrito no se vacía en un fallo: la venta no se dio por
      // realizada.
      expect(container.read(carritoProvider).estaVacio, isFalse);
    });
  });

  group('Respuesta perdida en venta online (Fase 2, PLAN_MEJORAS.md)', () {
    ProviderContainer construirContainer(_VentaApiServidorSimulado api) {
      final container = ProviderContainer(
        overrides: [
          backendAlcanzableProvider.overrideWith((ref) => Stream.value(true)),
          ventaApiProvider.overrideWithValue(api),
        ],
      );
      return container;
    }

    test('respuesta perdida justo después de crear(): el reintento con la '
        'misma clave no crea una segunda venta', () async {
      final api = _VentaApiServidorSimulado()
        ..perderRespuestaDelProximoCrear = true;
      final container = construirContainer(api);
      addTearDown(container.dispose);
      container.listen(backendAlcanzableProvider, (_, _) {});
      await container.read(backendAlcanzableProvider.future);

      const clave = 'venta-crear-perdido';
      container.read(carritoProvider.notifier).agregarProducto(_producto());

      // Intento 1: crear() se aplica server-side pero la respuesta se
      // pierde — confirmar() debe fallar (no puede saber que sí sirvió) y
      // dejar el carrito intacto para poder reintentar.
      await container
          .read(checkoutProvider.notifier)
          .confirmar(
            tiendaId: 1,
            metodo: MetodoPago.efectivo,
            clienteId: 42,
            correlationId: clave,
          );
      expect(container.read(checkoutProvider).error, isNotNull);
      expect(container.read(carritoProvider).estaVacio, isFalse);
      expect(api._ventas, hasLength(1));

      // Intento 2 (reintento manual, misma clave): crear() es idempotente
      // y devuelve la misma venta; completar() ahora sí llega a buen
      // puerto.
      await container
          .read(checkoutProvider.notifier)
          .confirmar(
            tiendaId: 1,
            metodo: MetodoPago.efectivo,
            clienteId: 42,
            correlationId: clave,
          );

      final estado = container.read(checkoutProvider);
      expect(estado.error, isNull);
      expect(estado.ventaCompletada, isTrue);
      expect(api._ventas, hasLength(1));
      expect(api._ventas.single.estado, 'COMPLETADA');
    });

    test(
      'respuesta perdida justo después de completar(): el reintento '
      'reconoce que la venta ya quedó COMPLETADA en vez de mostrar error',
      () async {
        final api = _VentaApiServidorSimulado()
          ..perderRespuestaDelProximoCompletar = true;
        final container = construirContainer(api);
        addTearDown(container.dispose);
        container.listen(backendAlcanzableProvider, (_, _) {});
        await container.read(backendAlcanzableProvider.future);

        const clave = 'venta-completar-perdido';
        container.read(carritoProvider.notifier).agregarProducto(_producto());

        // Intento 1: completar() sí marca la venta COMPLETADA server-side,
        // pero la respuesta se pierde — sin la reconciliación por
        // `ventaYaQuedoCompletada`, esto se vería como un fallo.
        await container
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: clave,
            );
        expect(container.read(checkoutProvider).error, isNotNull);
        expect(container.read(carritoProvider).estaVacio, isFalse);
        expect(api._ventas.single.estado, 'COMPLETADA');

        // Intento 2 (reintento manual, misma clave): crear() devuelve la
        // misma venta ya COMPLETADA; completar() de nuevo lanza
        // ESTADO_VENTA_INVALIDO, que CheckoutNotifier ahora reconcilia
        // consultando `obtener()` en vez de mostrarlo como error.
        await container
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: clave,
            );

        final estado = container.read(checkoutProvider);
        expect(estado.error, isNull);
        expect(estado.ventaCompletada, isTrue);
        expect(api._ventas, hasLength(1));
        expect(api.llamadasObtener, greaterThanOrEqualTo(1));
      },
    );
  });
}
