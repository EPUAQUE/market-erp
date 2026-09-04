import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import 'caja.dart';

class CajaApi {
  CajaApi(this._client);

  final ApiClient _client;

  /// `null` si la tienda no tiene ninguna caja abierta ahora mismo — no es un
  /// error, es el estado normal antes de que alguien abra turno.
  Future<CajaSesion?> obtenerAbierta(int tiendaId) async {
    try {
      return await _client.get<CajaSesion>(
        '/api/v1/caja/tiendas/$tiendaId/abierta',
        parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
      );
    } on ApiException catch (error) {
      if (error.status == 404) return null;
      rethrow;
    }
  }

  /// [correlationId] es opcional por compatibilidad con llamadores internos
  /// del service layer (ver `venta_api.dart`), pero `CajaActionsNotifier`
  /// siempre manda uno — evita que un reintento (respuesta perdida, o el
  /// propio `SyncEngine` reintentando un movimiento encolado) duplique el
  /// ingreso/egreso o la apertura/cierre en el servidor.
  Future<CajaSesion> abrir({
    required int tiendaId,
    required Decimal montoInicial,
    String? correlationId,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/abrir',
      data: {
        'montoInicial': montoInicial.toString(),
        'correlationId': correlationId,
      },
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }

  Future<CajaSesion> registrarMovimiento({
    required int tiendaId,
    required TipoMovimientoCaja tipo,
    required String concepto,
    required Decimal monto,
    String? correlationId,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/movimientos',
      data: {
        'tipo': tipoMovimientoCajaToJson(tipo),
        'concepto': concepto,
        'monto': monto.toString(),
        'correlationId': correlationId,
      },
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }

  Future<CajaSesion> cerrar({
    required int tiendaId,
    required Decimal montoFinalContado,
    String? correlationId,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/cerrar',
      data: {
        'montoFinalContado': montoFinalContado.toString(),
        'correlationId': correlationId,
      },
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }
}
