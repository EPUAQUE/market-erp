import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../../core/connectivity/backend_reachability_provider.dart';
import '../../../core/db/local_store_provider.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../clientes/application/clientes_provider.dart';
import '../data/venta_pendiente_local.dart';
import 'carrito_notifier.dart';
import '../data/venta_api.dart';
import '../domain/carrito.dart';

final ventaApiProvider = Provider<VentaApi>(
  (ref) => VentaApi(ApiClient.instance),
);
final cuentaPorCobrarApiProvider = Provider<CuentaPorCobrarApi>(
  (ref) => CuentaPorCobrarApi(ApiClient.instance),
);

/// Cliente "Consumidor Final" seedeado por el backend (`clientes/001-cliente.xml`)
/// — su id es estable en cualquier instalación fresca porque es el primer
/// registro insertado. Se usa como último recurso offline, donde no hay forma
/// de resolverlo por nombre contra la lista de clientes (esa lista también
/// requiere red). Ver CLAUDE.md.
const _clienteConsumidorFinalIdFallback = 1;

/// UUID v4 criptográficamente aleatorio para toda venta, online u offline —
/// antes solo la cola offline generaba uno (con
/// `DateTime.now().microsecondsSinceEpoch`, no aleatorio de verdad: dos
/// dispositivos con relojes cercanos podían, en teoría, colisionar). Expuesto
/// para que quien llama a [CheckoutNotifier.confirmar] (una sola vez por
/// intento de cobro, ver `CobroSheet`) genere la clave de idempotencia antes
/// del primer request y la reutilice si el usuario reintenta manualmente tras
/// un error — regenerarla en cada reintento anularía la protección contra
/// duplicados que esto existe para dar.
final _uuid = Uuid();

String nuevoCorrelationId() => _uuid.v4();

class CheckoutState {
  const CheckoutState({
    this.loading = false,
    this.error,
    this.ventaCompletada = false,
  });

  final bool loading;
  final String? error;
  final bool ventaCompletada;
}

/// Orquesta crear → completar. El backend ahora resuelve el ingreso
/// inmediato y la cuenta por cobrar dentro de `completar()` mismo (ver
/// CLAUDE.md, "Flujo contable correcto de ventas") — este cliente ya no
/// dispara `registrarCobro` por separado después de completar: para
/// EFECTIVO/TARJETA/TRANSFERENCIA el servidor resuelve el pago inmediato por
/// el total sin que el cliente mande nada; para CREDITO no hay pago
/// inmediato; para "Mixto" el [desglose] se manda directo en la misma
/// llamada a `completar()`, y el saldo no cubierto (si lo hay) genera una
/// cuenta por cobrar automáticamente — ya no hace falta cobrarlo aparte.
///
/// Sin conexión, la venta no se pierde: se guarda en el `LocalStore` y se
/// sincroniza sola cuando vuelva la red (ver `SyncEngine`) — el vendedor
/// nunca ve un error de red a mitad de una venta. "Mixto" exige red: la cola
/// offline (`NuevaVentaPendiente`) solo guarda un monto y un método por
/// venta, no un desglose (ver CLAUDE.md).
class CheckoutNotifier extends Notifier<CheckoutState> {
  @override
  CheckoutState build() => const CheckoutState();

  /// [correlationId]: una clave por intento de cobro, generada UNA VEZ por
  /// quien llama (ver `nuevoCorrelationId()` y `CobroSheet`, que la genera al
  /// abrir la hoja y la reutiliza en cada reintento) — nunca generada aquí
  /// adentro, porque regenerarla en cada llamada anularía la protección de
  /// idempotencia: un reintento manual tras un timeout debe mandar la MISMA
  /// clave que el intento original para que el backend lo reconozca como el
  /// mismo intento, no como una venta nueva.
  ///
  /// [clientePendienteLocalId]: id local de un cliente creado offline en esta
  /// misma sesión, todavía sin id real de servidor (ver
  /// `ClienteSeleccionado.pendienteLocal`). Si viene presente, la venta SIEMPRE
  /// se encola offline sin importar `hayRed` — el cliente no tiene id real
  /// todavía, así que no hay forma de mandarla al backend hasta que
  /// `SyncEngineNotifier` sincronice primero ese cliente y resuelva su id.
  Future<void> confirmar({
    required int tiendaId,
    required MetodoPago metodo,
    required String correlationId,
    int? clienteId,
    int? clientePendienteLocalId,
    Map<MetodoPago, Decimal>? desglose,
  }) async {
    final carrito = ref.read(carritoProvider);
    if (carrito.estaVacio) {
      state = const CheckoutState(error: 'El carrito está vacío.');
      return;
    }

    state = const CheckoutState(loading: true);
    try {
      final hayRed =
          (ref.read(backendAlcanzableProvider).value ?? true) &&
          clientePendienteLocalId == null;
      if (metodo == MetodoPago.mixto && !hayRed) {
        throw ApiException(
          message: 'El pago mixto requiere conexión a internet.',
        );
      }
      if (hayRed) {
        await _confirmarOnline(
          tiendaId: tiendaId,
          metodo: metodo,
          clienteId: clienteId,
          desglose: desglose,
          carrito: carrito,
          correlationId: correlationId,
        );
      } else {
        await _confirmarOffline(
          tiendaId: tiendaId,
          metodo: metodo,
          clienteId: clienteId,
          clientePendienteLocalId: clientePendienteLocalId,
          carrito: carrito,
          correlationId: correlationId,
        );
      }

      ref.read(carritoProvider.notifier).vaciar();
      state = const CheckoutState(ventaCompletada: true);
    } on ApiException catch (error) {
      state = CheckoutState(error: error.message);
    } catch (_) {
      state = const CheckoutState(error: 'No se pudo completar la venta.');
    }
  }

  Future<void> _confirmarOnline({
    required int tiendaId,
    required MetodoPago metodo,
    required String correlationId,
    int? clienteId,
    Map<MetodoPago, Decimal>? desglose,
    required CarritoState carrito,
  }) async {
    final clienteFinal = clienteId ?? await _clienteConsumidorFinal();
    final ventaApi = ref.read(ventaApiProvider);
    final creada = await ventaApi.crear(
      tiendaId: tiendaId,
      clienteId: clienteFinal,
      lineas: carrito.lineas,
      metodoPago: metodo,
      correlationId: correlationId,
    );
    await ventaApi.completar(
      tiendaId: tiendaId,
      ventaId: creada.id,
      pagosInmediatos: metodo == MetodoPago.mixto ? desglose : null,
    );
  }

  Future<void> _confirmarOffline({
    required int tiendaId,
    required MetodoPago metodo,
    required String correlationId,
    int? clienteId,
    int? clientePendienteLocalId,
    required CarritoState carrito,
  }) async {
    final store = await ref.read(localStoreProvider.future);
    if (!store.disponible) {
      throw ApiException(
        message:
            'Sin conexión y sin almacenamiento local disponible en este dispositivo.',
      );
    }
    await store.encolarVentaPendiente(
      NuevaVentaPendiente(
        correlationId: correlationId,
        tiendaId: tiendaId,
        // Exactamente uno de los dos: si hay un cliente pendiente de
        // sincronizar, esta venta lo referencia por su id local y espera a
        // que SyncEngineNotifier resuelva el id real — nunca cae en
        // Consumidor Final solo porque clienteId venga nulo en ese caso.
        clienteId: clientePendienteLocalId == null
            ? (clienteId ?? _clienteConsumidorFinalIdFallback)
            : null,
        clientePendienteLocalId: clientePendienteLocalId,
        lineas: carrito.lineas,
        metodoPago: metodo.name,
        montoACobrar: metodo == MetodoPago.credito ? null : carrito.total,
        creadaEn: DateTime.now(),
      ),
    );
  }

  Future<int> _clienteConsumidorFinal() async {
    final clientes = await ref.read(clientesProvider.future);
    for (final cliente in clientes) {
      if (cliente.nombre == 'Consumidor Final') return cliente.id;
    }
    throw ApiException(
      message:
          'No se encontró el cliente Consumidor Final. Selecciona un cliente.',
    );
  }

  void reset() => state = const CheckoutState();
}

final checkoutProvider = NotifierProvider<CheckoutNotifier, CheckoutState>(
  CheckoutNotifier.new,
);
