import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/connectivity/connectivity_provider.dart';
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

  Future<void> confirmar({
    required int tiendaId,
    required MetodoPago metodo,
    int? clienteId,
    Map<MetodoPago, Decimal>? desglose,
  }) async {
    final carrito = ref.read(carritoProvider);
    if (carrito.estaVacio) {
      state = const CheckoutState(error: 'El carrito está vacío.');
      return;
    }

    state = const CheckoutState(loading: true);
    try {
      final hayRed = ref.read(redDisponibleProvider).value ?? true;
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
        );
      } else {
        await _confirmarOffline(
          tiendaId: tiendaId,
          metodo: metodo,
          clienteId: clienteId,
          carrito: carrito,
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
    int? clienteId,
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
        correlationId: '${DateTime.now().microsecondsSinceEpoch}-$tiendaId',
        tiendaId: tiendaId,
        clienteId: clienteId ?? _clienteConsumidorFinalIdFallback,
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
