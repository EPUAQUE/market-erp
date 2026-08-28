import 'dart:async';

import 'package:decimal/decimal.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/caja/application/caja_provider.dart';
import '../../features/caja/data/movimiento_caja_pendiente_local.dart';
import '../../features/clientes/application/clientes_provider.dart';
import '../../features/clientes/data/cliente_pendiente_local.dart';
import '../../features/ventas/application/checkout_notifier.dart';
import '../../features/ventas/data/venta.dart';
import '../../features/ventas/data/venta_api.dart';
import '../../features/ventas/data/venta_pendiente_local.dart';
import '../connectivity/backend_reachability_provider.dart';
import '../connectivity/connectivity_provider.dart';
import '../db/local_store.dart';
import '../db/local_store_provider.dart';
import '../network/api_exception.dart';

/// Drena las colas offline (clientes, ventas, movimientos de caja) al
/// reconectar — cada una FIFO por `creadaEn`, y las tres son independientes
/// entre sí (una venta offline nunca referencia un cliente offline todavía
/// sin sincronizar — ver `ClienteSelectorSheet`/CLAUDE.md — así que el orden
/// entre colas no importa). Un fallo de *red* detiene el drenado de ESA cola
/// (se reintenta en la próxima reconexión, sin perder el orden); un fallo de
/// *negocio* (ej. producto ya no vendible, cliente duplicado) marca ese
/// ítem con `mensajeError` y sigue con los siguientes — no bloquea la cola
/// por un ítem que un encargado debe revisar a mano.
class SyncEngineNotifier extends Notifier<EstadoConexion> {
  bool _drenando = false;

  @override
  EstadoConexion build() {
    // Escucha alcanzabilidad real del backend, no solo la interfaz de red:
    // "Wi-Fi activo con backend caído" ahora también deja el estado en
    // `sinConexion` (antes se quedaba `conectado` sin intentar drenar nunca,
    // porque `redDisponibleProvider` no distinguía ese caso).
    ref.listen(backendAlcanzableProvider, (previous, next) {
      final alcanzable = next.value;
      if (alcanzable == null) return;
      if (alcanzable) {
        _drenarCola();
      } else {
        state = EstadoConexion.sinConexion;
      }
    });
    final actual = ref.read(backendAlcanzableProvider).value ?? false;
    return actual ? EstadoConexion.conectado : EstadoConexion.sinConexion;
  }

  Future<void> _drenarCola() async {
    if (_drenando) return;
    _drenando = true;
    state = EstadoConexion.sincronizando;
    try {
      final store = await ref.read(localStoreProvider.future);
      if (!store.disponible) {
        state = EstadoConexion.conectado;
        return;
      }
      final clientesPendientes = await store.listarClientesPendientes();
      for (final cliente in clientesPendientes) {
        final exito = await _sincronizarCliente(store, cliente);
        if (!exito) break;
      }

      final pendientes = await store.listarVentasPendientes();
      for (final venta in pendientes) {
        final exito = await _sincronizarVenta(store, venta);
        if (!exito) break;
      }

      final movimientosPendientes = await store
          .listarMovimientosCajaPendientes();
      for (final movimiento in movimientosPendientes) {
        final exito = await _sincronizarMovimientoCaja(store, movimiento);
        if (!exito) break;
      }

      state = EstadoConexion.conectado;
    } finally {
      _drenando = false;
      ref.invalidate(pendientesConErrorProvider);
    }
  }

  /// Limpia `mensajeError` del ítem y dispara un drenado inmediato — no hace
  /// falta esperar al próximo evento de reconexión para que se reintente.
  Future<void> reintentar(ItemPendienteConError item) async {
    final store = await ref.read(localStoreProvider.future);
    switch (item.tipo) {
      case TipoPendiente.venta:
        await store.reintentarVentaPendiente(item.id);
      case TipoPendiente.movimientoCaja:
        await store.reintentarMovimientoCajaPendiente(item.id);
      case TipoPendiente.cliente:
        await store.reintentarClientePendiente(item.id);
    }
    ref.invalidate(pendientesConErrorProvider);
    ref.invalidate(pendientesSincronizarProvider);
    unawaited(_drenarCola());
  }

  /// Descarta el ítem sin volver a intentarlo — se pierde (no hay
  /// "recuperar"), por eso la pantalla exige confirmación antes de llamar
  /// esto.
  Future<void> descartar(ItemPendienteConError item) async {
    final store = await ref.read(localStoreProvider.future);
    switch (item.tipo) {
      case TipoPendiente.venta:
        await store.eliminarVentaPendiente(item.id);
      case TipoPendiente.movimientoCaja:
        await store.eliminarMovimientoCajaPendiente(item.id);
      case TipoPendiente.cliente:
        await store.eliminarClientePendiente(item.id);
    }
    ref.invalidate(pendientesConErrorProvider);
    ref.invalidate(pendientesSincronizarProvider);
  }

  Future<bool> _sincronizarVenta(
    LocalStore store,
    VentaPendienteLocal venta,
  ) async {
    final ventaApi = ref.read(ventaApiProvider);
    final Venta creada;
    try {
      creada = await ventaApi.crear(
        tiendaId: venta.tiendaId,
        clienteId: venta.clienteId,
        lineas: venta.lineas,
        metodoPago: metodoPagoFromJson(venta.metodoPago),
        correlationId: venta.correlationId,
      );
    } on ApiException catch (error) {
      if (error.isNetworkError) return false;
      await store.marcarVentaPendienteConError(venta.id, error.message);
      return true;
    }

    try {
      // `completar()` ya resuelve el ingreso inmediato y la cuenta por
      // cobrar por sí mismo según el método de pago (ver CLAUDE.md, "Flujo
      // contable correcto de ventas") — la cola offline no soporta Mixto
      // (ver `CheckoutNotifier`), así que nunca hace falta mandar desglose
      // acá: EFECTIVO/TARJETA/TRANSFERENCIA se cobran por el total sin más,
      // CREDITO no tiene pago inmediato.
      await ventaApi.completar(tiendaId: venta.tiendaId, ventaId: creada.id);
    } on ApiException catch (error) {
      if (error.isNetworkError) return false;
      if (error.code == 'ESTADO_VENTA_INVALIDO' &&
          await _yaQuedoCompletada(ventaApi, venta.tiendaId, creada.id)) {
        // Un `completar()` de un intento anterior probablemente sí tuvo
        // éxito en el servidor y solo se perdió la respuesta por la red —
        // reintentar completar() sobre una venta ya COMPLETADA lanza este
        // mismo error de negocio, que sin esta comprobación se marcaba como
        // fallo permanente aunque la venta ya estuviera bien.
        await store.eliminarVentaPendiente(venta.id);
        return true;
      }
      await store.marcarVentaPendienteConError(venta.id, error.message);
      return true;
    }

    await store.eliminarVentaPendiente(venta.id);
    return true;
  }

  /// `null` (red caída al confirmar) se trata igual que "no, no está
  /// completada" — el llamador la marca con error y un encargado la revisa a
  /// mano; más seguro que asumir éxito sin poder confirmarlo.
  Future<bool> _yaQuedoCompletada(
    VentaApi ventaApi,
    int tiendaId,
    int ventaId,
  ) async {
    try {
      final actual = await ventaApi.obtener(tiendaId: tiendaId, ventaId: ventaId);
      return actual.estado == 'COMPLETADA';
    } on ApiException {
      return false;
    }
  }

  Future<bool> _sincronizarCliente(
    LocalStore store,
    ClientePendienteLocal cliente,
  ) async {
    try {
      await ref
          .read(clientesApiProvider)
          .crear(
            nombre: cliente.nombre,
            telefono: cliente.telefono,
            nit: cliente.nit,
            limiteCredito: cliente.limiteCredito,
          );
      ref.invalidate(clientesProvider);
      await store.eliminarClientePendiente(cliente.id);
      return true;
    } on ApiException catch (error) {
      if (error.isNetworkError) return false;
      await store.marcarClientePendienteConError(cliente.id, error.message);
      return true;
    }
  }

  Future<bool> _sincronizarMovimientoCaja(
    LocalStore store,
    MovimientoCajaPendienteLocal movimiento,
  ) async {
    try {
      await ref
          .read(cajaApiProvider)
          .registrarMovimiento(
            tiendaId: movimiento.tiendaId,
            tipo: movimiento.tipo,
            concepto: movimiento.concepto,
            monto: movimiento.monto,
          );
      ref.invalidate(cajaAbiertaProvider(movimiento.tiendaId));
      await store.eliminarMovimientoCajaPendiente(movimiento.id);
      return true;
    } on ApiException catch (error) {
      if (error.isNetworkError) return false;
      await store.marcarMovimientoCajaPendienteConError(
        movimiento.id,
        error.message,
      );
      return true;
    }
  }
}

final syncEngineProvider = NotifierProvider<SyncEngineNotifier, EstadoConexion>(
  SyncEngineNotifier.new,
);

/// Cuántos ítems (ventas + clientes + movimientos de caja) siguen esperando
/// sincronizar en total (incluye los marcados con error, que un encargado
/// debe revisar manualmente — no se reintentan solos).
final pendientesSincronizarProvider = FutureProvider<int>((ref) async {
  final store = await ref.watch(localStoreProvider.future);
  final ventas = await store.contarVentasPendientes();
  final clientes = await store.contarClientesPendientes();
  final movimientos = await store.contarMovimientosCajaPendientes();
  return ventas + clientes + movimientos;
});

enum TipoPendiente { venta, movimientoCaja, cliente }

/// Vista aplanada de los 3 tipos de ítem con `mensajeError` — lo que ve
/// `PendientesErrorScreen`. No lleva la entidad original completa porque cada
/// tipo tiene forma distinta; solo lo necesario para mostrar la tarjeta y
/// disparar reintentar/descartar por `tipo`+`id`.
class ItemPendienteConError {
  const ItemPendienteConError({
    required this.tipo,
    required this.id,
    required this.titulo,
    required this.subtitulo,
    required this.mensajeError,
    required this.creadaEn,
  });

  final TipoPendiente tipo;
  final int id;
  final String titulo;
  final String subtitulo;
  final String mensajeError;
  final DateTime creadaEn;
}

final pendientesConErrorProvider = FutureProvider<List<ItemPendienteConError>>((
  ref,
) async {
  final store = await ref.watch(localStoreProvider.future);
  final ventas = await store.listarVentasPendientesConError();
  final movimientos = await store.listarMovimientosCajaPendientesConError();
  final clientes = await store.listarClientesPendientesConError();

  final items = [
    for (final v in ventas)
      ItemPendienteConError(
        tipo: TipoPendiente.venta,
        id: v.id,
        titulo: 'Venta — Q ${_totalVenta(v)}',
        subtitulo: '${v.lineas.length} línea(s) · ${v.metodoPago}',
        mensajeError: v.mensajeError ?? '',
        creadaEn: v.creadaEn,
      ),
    for (final m in movimientos)
      ItemPendienteConError(
        tipo: TipoPendiente.movimientoCaja,
        id: m.id,
        titulo: 'Movimiento de caja — Q ${m.monto}',
        subtitulo: m.concepto,
        mensajeError: m.mensajeError ?? '',
        creadaEn: m.creadaEn,
      ),
    for (final c in clientes)
      ItemPendienteConError(
        tipo: TipoPendiente.cliente,
        id: c.id,
        titulo: 'Cliente nuevo — ${c.nombre}',
        subtitulo: c.nit ?? c.telefono ?? 'Sin NIT ni teléfono',
        mensajeError: c.mensajeError ?? '',
        creadaEn: c.creadaEn,
      ),
  ];
  items.sort((a, b) => a.creadaEn.compareTo(b.creadaEn));
  return items;
});

Decimal _totalVenta(VentaPendienteLocal venta) => venta.lineas.fold(
  Decimal.zero,
  (total, linea) => total + linea.precioUnitario * linea.cantidad,
);
