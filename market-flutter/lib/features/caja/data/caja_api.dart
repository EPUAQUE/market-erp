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

  Future<CajaSesion> abrir({
    required int tiendaId,
    required Decimal montoInicial,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/abrir',
      data: {'montoInicial': montoInicial.toString()},
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }

  Future<CajaSesion> registrarMovimiento({
    required int tiendaId,
    required TipoMovimientoCaja tipo,
    required String concepto,
    required Decimal monto,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/movimientos',
      data: {
        'tipo': tipoMovimientoCajaToJson(tipo),
        'concepto': concepto,
        'monto': monto.toString(),
      },
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }

  Future<CajaSesion> cerrar({
    required int tiendaId,
    required Decimal montoFinalContado,
  }) {
    return _client.post<CajaSesion>(
      '/api/v1/caja/tiendas/$tiendaId/cerrar',
      data: {'montoFinalContado': montoFinalContado.toString()},
      parser: (data) => CajaSesion.fromJson(data as Map<String, dynamic>),
    );
  }
}
