import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/paginacion.dart';

class CuentaPorPagar {
  const CuentaPorPagar({
    required this.id,
    required this.compraId,
    required this.proveedorId,
    required this.saldoPendiente,
    required this.estado,
    required this.fechaVencimiento,
  });

  factory CuentaPorPagar.fromJson(Map<String, dynamic> json) {
    return CuentaPorPagar(
      id: json['id'] as int,
      compraId: json['compraId'] as int,
      proveedorId: json['proveedorId'] as int,
      saldoPendiente: Decimal.parse(json['saldoPendiente'] as String),
      estado: json['estado'] as String,
      fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
    );
  }

  final int id;
  final int compraId;
  final int proveedorId;
  final Decimal saldoPendiente;

  /// String cruda del backend (`PENDIENTE`/`PAGADA`/`ANULADA`) — mismo
  /// criterio que `CuentaPorCobrar.estado` (ver ese archivo).
  final String estado;
  final DateTime fechaVencimiento;

  bool get pendiente => estado == 'PENDIENTE';

  bool get vencida => pendiente && fechaVencimiento.isBefore(DateTime.now());
}

class CuentaPorPagarApi {
  CuentaPorPagarApi(this._client);

  final ApiClient _client;

  /// Igual que `CuentaPorCobrarApi.listarPorTienda` — envelope paginado, se
  /// pide la página completa porque esta pantalla no pagina de verdad.
  Future<List<CuentaPorPagar>> listarPorTienda(int tiendaId) {
    return _client.get<List<CuentaPorPagar>>(
      '/api/v1/cuentas-por-pagar/tiendas/$tiendaId',
      query: {'size': tamanoPaginaCompleta},
      parser: (data) => contenidoDePagina(data)
          .map((json) => CuentaPorPagar.fromJson(json as Map<String, dynamic>))
          .toList(),
    );
  }

  /// A diferencia de `CuentaPorCobrarApi.registrarCobro`, el backend no pide
  /// `metodoPago` acá (`RegistrarPagoRequest` solo tiene `monto`) — un pago a
  /// proveedor no distingue canal en este módulo todavía.
  Future<void> registrarPago({
    required int tiendaId,
    required int cuentaId,
    required Decimal monto,
  }) {
    return _client.post<void>(
      '/api/v1/cuentas-por-pagar/tiendas/$tiendaId/$cuentaId/pagos',
      data: {'monto': monto.toString()},
      parser: (_) {},
    );
  }

  /// El backend solo permite anular una cuenta `PENDIENTE` sin pagos
  /// registrados — esta pantalla no filtra eso de antemano, deja que el
  /// backend lo rechace y muestra el error (mismo patrón que Cuentas por
  /// Cobrar).
  Future<void> anular({required int tiendaId, required int cuentaId}) {
    return _client.post<void>(
      '/api/v1/cuentas-por-pagar/tiendas/$tiendaId/$cuentaId/anular',
      parser: (_) {},
    );
  }
}
