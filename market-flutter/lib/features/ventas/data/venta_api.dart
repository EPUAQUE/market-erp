import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/paginacion.dart';
import '../domain/carrito.dart';
import 'venta.dart';

class VentaApi {
  VentaApi(this._client);

  final ApiClient _client;

  /// [correlationId] es opcional — solo lo manda `SyncEngine` al sincronizar
  /// una venta creada offline, para que un reintento no cree una venta
  /// duplicada si la respuesta de un intento anterior no llegó a tiempo
  /// (idempotencia — ver CLAUDE.md).
  Future<Venta> crear({
    required int tiendaId,
    required int clienteId,
    required List<LineaCarrito> lineas,
    required MetodoPago metodoPago,
    String? correlationId,
  }) {
    return _client.post<Venta>(
      '/api/v1/ventas/tiendas/$tiendaId',
      data: {
        'clienteId': clienteId,
        'lineas': lineas
            .map(
              (l) => {
                'productoId': l.productoId,
                'cantidad': l.cantidad.toString(),
                'precioUnitario': l.precioUnitario.toString(),
              },
            )
            .toList(),
        'metodoPago': metodoPagoToJson(metodoPago),
        'correlationId': correlationId,
      },
      parser: (data) => Venta.fromJson(data as Map<String, dynamic>),
    );
  }

  /// [pagosInmediatos] solo hace falta (y se valida) para una venta MIXTO —
  /// el backend resuelve el desglose por sí mismo para cualquier otro método
  /// (ver CLAUDE.md, "Flujo contable correcto de ventas").
  Future<Venta> completar({
    required int tiendaId,
    required int ventaId,
    Map<MetodoPago, Decimal>? pagosInmediatos,
  }) {
    final pagos = (pagosInmediatos ?? {}).entries
        .where((e) => e.value > Decimal.zero)
        .map(
          (e) => {
            'metodoPago': metodoPagoToJson(e.key),
            'monto': e.value.toString(),
          },
        )
        .toList();
    return _client.post<Venta>(
      '/api/v1/ventas/tiendas/$tiendaId/$ventaId/completar',
      data: pagos.isEmpty ? null : {'pagos': pagos},
      parser: (data) => Venta.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Usado por `SyncEngine` para distinguir, tras un `ESTADO_VENTA_INVALIDO`
  /// de [completar], si la venta ya quedó `COMPLETADA` de verdad (el
  /// `completar()` anterior sí tuvo éxito en el servidor, solo se perdió la
  /// respuesta por la red) de un estado genuinamente inválido (ej. `ANULADA`).
  Future<Venta> obtener({required int tiendaId, required int ventaId}) {
    return _client.get<Venta>(
      '/api/v1/ventas/tiendas/$tiendaId/$ventaId',
      parser: (data) => Venta.fromJson(data as Map<String, dynamic>),
    );
  }
}

/// Método de pago elegido en la hoja de cobro. El backend lo persiste en
/// `Venta.metodoPago` (ver CLAUDE.md) — sigue sin cambiar ninguna regla de
/// negocio acá: "crédito" sigue siendo "no cobrar de inmediato", el resto
/// sigue cobrándose completo al completar la venta.
enum MetodoPago { efectivo, tarjeta, transferencia, credito, mixto }

String metodoPagoToJson(MetodoPago metodo) => metodo.name.toUpperCase();

/// Case-insensitive a propósito: el `LocalStore` guarda `metodo.name` en
/// minúscula (ver `checkout_notifier.dart`), mientras que el backend usa
/// mayúscula — esta función es el único punto que reconstruye el enum en
/// ambos casos, así que no vale la pena tener dos representaciones de texto.
MetodoPago metodoPagoFromJson(String? value) {
  return MetodoPago.values.firstWhere(
    (m) => m.name.toLowerCase() == value?.toLowerCase(),
    orElse: () => MetodoPago.efectivo,
  );
}

class CuentaPorCobrar {
  const CuentaPorCobrar({
    required this.id,
    required this.ventaId,
    required this.clienteId,
    required this.saldoPendiente,
    required this.estado,
    required this.fechaVencimiento,
  });

  factory CuentaPorCobrar.fromJson(Map<String, dynamic> json) {
    return CuentaPorCobrar(
      id: json['id'] as int,
      ventaId: json['ventaId'] as int,
      clienteId: json['clienteId'] as int,
      saldoPendiente: Decimal.parse(json['saldoPendiente'] as String),
      estado: json['estado'] as String,
      fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
    );
  }

  final int id;
  final int ventaId;
  final int clienteId;
  final Decimal saldoPendiente;

  /// String cruda del backend (`PENDIENTE`/`COBRADA`/`ANULADA`) — no vale la
  /// pena un enum Dart para 3 valores usados en un solo filtro.
  final String estado;
  final DateTime fechaVencimiento;

  bool get pendiente => estado == 'PENDIENTE';

  bool get vencida => pendiente && fechaVencimiento.isBefore(DateTime.now());
}

class CuentaPorCobrarApi {
  CuentaPorCobrarApi(this._client);

  final ApiClient _client;

  /// El endpoint ahora devuelve un envelope paginado (ver
  /// `core/network/paginacion.dart`); se pide `tamanoPaginaCompleta` porque
  /// `buscarPorVenta` de abajo necesita la lista completa para su búsqueda
  /// O(n) — no hay filtro por `ventaId` en el backend.
  Future<List<CuentaPorCobrar>> listarPorTienda(int tiendaId) {
    return _client.get<List<CuentaPorCobrar>>(
      '/api/v1/cuentas-por-cobrar/tiendas/$tiendaId',
      query: {'size': tamanoPaginaCompleta},
      parser: (data) => contenidoDePagina(data)
          .map((json) => CuentaPorCobrar.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }

  /// [metodoPago] es el canal concreto de este abono — nunca
  /// `MetodoPago.credito` ni `MetodoPago.mixto` (esos son la intención de la
  /// venta, no un canal real; ver CLAUDE.md).
  Future<void> registrarCobro({
    required int tiendaId,
    required int cuentaId,
    required Decimal monto,
    required MetodoPago metodoPago,
  }) {
    return _client.post<void>(
      '/api/v1/cuentas-por-cobrar/tiendas/$tiendaId/$cuentaId/cobros',
      data: {
        'monto': monto.toString(),
        'metodoPago': metodoPagoToJson(metodoPago),
      },
      parser: (_) {},
    );
  }

  /// El backend solo permite anular una cuenta `PENDIENTE` sin cobros
  /// registrados (`CuentaConCobrosException` si ya tiene alguno) — esta
  /// pantalla no filtra eso de antemano, deja que el backend lo rechace y
  /// muestra el error (ver CLAUDE.md).
  Future<void> anular({required int tiendaId, required int cuentaId}) {
    return _client.post<void>(
      '/api/v1/cuentas-por-cobrar/tiendas/$tiendaId/$cuentaId/anular',
      parser: (_) {},
    );
  }

  /// La cuenta por cobrar de una venta recién completada no se puede pedir
  /// por `ventaId` directamente — el backend solo expone `listar`/`obtener
  /// por id propio` (ver CLAUDE.md). Para una tienda con volumen alto de
  /// ventas, esto se vuelve una búsqueda O(n) — el fix real es un endpoint
  /// `GET .../por-venta/{ventaId}`, no algo que este cliente pueda arreglar.
  Future<CuentaPorCobrar?> buscarPorVenta({
    required int tiendaId,
    required int ventaId,
  }) async {
    final cuentas = await listarPorTienda(tiendaId);
    for (final cuenta in cuentas) {
      if (cuenta.ventaId == ventaId) return cuenta;
    }
    return null;
  }
}
