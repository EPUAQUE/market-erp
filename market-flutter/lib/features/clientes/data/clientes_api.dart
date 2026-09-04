import 'package:decimal/decimal.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/paginacion.dart';
import 'cliente.dart';

class ClientesApi {
  ClientesApi(this._client);

  final ApiClient _client;

  /// El POS necesita la lista completa (resolución de "Consumidor Final",
  /// búsqueda O(n) en `ClienteSelectorSheet`), no paginar de verdad — mismo
  /// patrón que `ProductosApi`/`CuentaPorCobrarApi` (ver
  /// `core/network/paginacion.dart`). Antes de este fix pedía la página sin
  /// `size` y trataba el envelope `{contenido, pagina, ...}` como si fuera un
  /// array plano, lo que rompía con un `TypeError` en cuanto había algún
  /// cliente (incluido el "Consumidor Final" seedeado) — toda venta online
  /// sin cliente explícito fallaba.
  Future<List<Cliente>> listar() {
    return _client.get<List<Cliente>>(
      '/api/v1/clientes',
      query: {'size': tamanoPaginaCompleta},
      parser: (data) => contenidoDePagina(
        data,
      ).map((json) => Cliente.fromJson(json as Map<String, dynamic>)).toList(),
    );
  }

  /// Resuelve "Consumidor Final" por nombre en el servidor (Fase 2,
  /// PLAN_MEJORAS.md) — ya no se asume que su id de fila sea `1`, un accidente
  /// de orden de migración, no un contrato. Ver `checkout_notifier.dart` para
  /// el fallback offline (sin red, sin forma de golpear este endpoint).
  Future<Cliente> obtenerConsumidorFinal() {
    return _client.get<Cliente>(
      '/api/v1/clientes/consumidor-final',
      parser: (data) => Cliente.fromJson(data as Map<String, dynamic>),
    );
  }

  /// [correlationId] evita que un reintento (respuesta perdida, o el propio
  /// `SyncEngine` reintentando un alta encolada sin conexión) cree un
  /// segundo cliente en el servidor — ver `core/util/correlation_id.dart`.
  Future<Cliente> crear({
    required String nombre,
    String? telefono,
    String? nit,
    Decimal? limiteCredito,
    String? correlationId,
  }) {
    return _client.post<Cliente>(
      '/api/v1/clientes',
      data: {
        'nombre': nombre,
        'telefono': telefono,
        'nit': nit,
        'limiteCredito': limiteCredito?.toString(),
        'correlationId': correlationId,
      },
      parser: (data) => Cliente.fromJson(data as Map<String, dynamic>),
    );
  }
}
