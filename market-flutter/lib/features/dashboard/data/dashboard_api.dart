import '../../../core/network/api_client.dart';
import 'dashboard_resumen.dart';

class DashboardApi {
  DashboardApi(this._client);

  final ApiClient _client;

  Future<DashboardResumen> obtenerResumen(int tiendaId) {
    return _client.get<DashboardResumen>(
      '/api/v1/dashboard/tiendas/$tiendaId',
      parser: (data) => DashboardResumen.fromJson(data as Map<String, dynamic>),
    );
  }
}
