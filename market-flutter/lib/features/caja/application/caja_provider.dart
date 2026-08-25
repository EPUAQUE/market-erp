import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/connectivity/connectivity_provider.dart';
import '../../../core/db/local_store_provider.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../data/caja.dart';
import '../data/caja_api.dart';
import '../data/movimiento_caja_pendiente_local.dart';

final cajaApiProvider = Provider<CajaApi>((ref) => CajaApi(ApiClient.instance));

final cajaAbiertaProvider = FutureProvider.family<CajaSesion?, int>((
  ref,
  tiendaId,
) {
  return ref.watch(cajaApiProvider).obtenerAbierta(tiendaId);
});

class CajaActionsState {
  const CajaActionsState({this.loading = false, this.error});

  final bool loading;
  final String? error;
}

class CajaActionsNotifier extends Notifier<CajaActionsState> {
  @override
  CajaActionsState build() => const CajaActionsState();

  Future<bool> abrir({required int tiendaId, required Decimal montoInicial}) {
    return _ejecutar(
      tiendaId,
      () => ref
          .read(cajaApiProvider)
          .abrir(tiendaId: tiendaId, montoInicial: montoInicial),
    );
  }

  /// Sin conexión, el movimiento no se pierde: se guarda en el `LocalStore`
  /// y se sincroniza solo cuando vuelva la red (ver `SyncEngine`) — mismo
  /// patrón que `CheckoutNotifier` para ventas offline. A diferencia de una
  /// venta, un movimiento offline no se refleja de inmediato en
  /// `cajaAbiertaProvider` (esa vista viene del servidor) — el saldo y la
  /// lista de movimientos del turno se actualizan recién al sincronizar.
  Future<bool> registrarMovimiento({
    required int tiendaId,
    required TipoMovimientoCaja tipo,
    required String concepto,
    required Decimal monto,
  }) async {
    final hayRed = ref.read(redDisponibleProvider).value ?? true;
    if (!hayRed) {
      return _registrarMovimientoOffline(
        tiendaId: tiendaId,
        tipo: tipo,
        concepto: concepto,
        monto: monto,
      );
    }
    return _ejecutar(
      tiendaId,
      () => ref
          .read(cajaApiProvider)
          .registrarMovimiento(
            tiendaId: tiendaId,
            tipo: tipo,
            concepto: concepto,
            monto: monto,
          ),
    );
  }

  Future<bool> _registrarMovimientoOffline({
    required int tiendaId,
    required TipoMovimientoCaja tipo,
    required String concepto,
    required Decimal monto,
  }) async {
    state = const CajaActionsState(loading: true);
    try {
      final store = await ref.read(localStoreProvider.future);
      if (!store.disponible) {
        state = const CajaActionsState(
          error:
              'Sin conexión y sin almacenamiento local disponible en este dispositivo.',
        );
        return false;
      }
      await store.encolarMovimientoCajaPendiente(
        NuevoMovimientoCajaPendiente(
          tiendaId: tiendaId,
          tipo: tipo,
          concepto: concepto,
          monto: monto,
          creadaEn: DateTime.now(),
        ),
      );
      state = const CajaActionsState();
      return true;
    } catch (_) {
      state = const CajaActionsState(
        error: 'No se pudo guardar el movimiento sin conexión.',
      );
      return false;
    }
  }

  Future<bool> cerrar({
    required int tiendaId,
    required Decimal montoFinalContado,
  }) {
    return _ejecutar(
      tiendaId,
      () => ref
          .read(cajaApiProvider)
          .cerrar(tiendaId: tiendaId, montoFinalContado: montoFinalContado),
    );
  }

  Future<bool> _ejecutar(
    int tiendaId,
    Future<CajaSesion> Function() accion,
  ) async {
    state = const CajaActionsState(loading: true);
    try {
      await accion();
      ref.invalidate(cajaAbiertaProvider(tiendaId));
      state = const CajaActionsState();
      return true;
    } on ApiException catch (error) {
      state = CajaActionsState(error: error.message);
      return false;
    } catch (_) {
      state = const CajaActionsState(
        error: 'No se pudo completar la operación.',
      );
      return false;
    }
  }
}

final cajaActionsProvider =
    NotifierProvider<CajaActionsNotifier, CajaActionsState>(
      CajaActionsNotifier.new,
    );
