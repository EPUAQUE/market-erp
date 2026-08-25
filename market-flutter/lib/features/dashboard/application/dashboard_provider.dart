import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/dashboard_api.dart';
import '../data/dashboard_resumen.dart';

final dashboardApiProvider = Provider<DashboardApi>(
  (ref) => DashboardApi(ApiClient.instance),
);

/// Resumen de tienda para el dashboard — ver CLAUDE.md: es agregado por
/// tienda, no por vendedor (el backend no expone `usuarioId` en `/me` ni un
/// filtro por vendedor en ventas, así que no hay forma correcta de recortar
/// esto a "mis ventas" todavía). No es `autoDispose`: se refresca con
/// `ref.invalidate(dashboardResumenProvider(tiendaId))` al reingresar a la
/// pantalla o al pull-to-refresh, igual que `cajaAbiertaProvider`.
final dashboardResumenProvider = FutureProvider.family<DashboardResumen, int>((
  ref,
  tiendaId,
) {
  return ref.watch(dashboardApiProvider).obtenerResumen(tiendaId);
});
