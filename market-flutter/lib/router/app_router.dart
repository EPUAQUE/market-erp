import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../features/auth/application/auth_notifier.dart';
import '../features/auth/presentation/login_screen.dart';
import '../features/auth/presentation/tienda_picker_screen.dart';
import '../features/caja/presentation/caja_screen.dart';
import '../features/cuentas_por_cobrar/presentation/cuentas_por_cobrar_screen.dart';
import '../features/dashboard/presentation/dashboard_router_screen.dart';
import '../features/sync/presentation/pendientes_error_screen.dart';
import '../features/ventas/presentation/pos_screen.dart';

class RouterRefreshNotifier extends ChangeNotifier {
  RouterRefreshNotifier(Ref ref) {
    ref.listen(authNotifierProvider, (_, _) => notifyListeners());
    ref.listen(tiendaActivaProvider, (_, _) => notifyListeners());
  }
}

/// Único guard de navegación: sesión + tienda elegida. La ruta inicial tras
/// login es siempre `/pos` — nunca un dashboard (ver CLAUDE.md).
final routerProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = RouterRefreshNotifier(ref);
  ref.onDispose(refreshNotifier.dispose);

  return GoRouter(
    initialLocation: '/login',
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final authState = ref.read(authNotifierProvider);
      if (authState.isLoading) return null;

      final sesion = authState.value;
      final isLoggingIn = state.matchedLocation == '/login';
      final isPickingTienda = state.matchedLocation == '/tienda';

      if (sesion == null) {
        return isLoggingIn ? null : '/login';
      }

      final tiendaActiva = ref.read(tiendaActivaProvider);
      if (tiendaActiva == null) {
        return isPickingTienda ? null : '/tienda';
      }

      if (isLoggingIn || isPickingTienda) return '/pos';

      if (state.matchedLocation == '/caja' && !sesion.can('CAJA_VER')) {
        return '/pos';
      }

      if (state.matchedLocation == '/dashboard' &&
          !sesion.can('DASHBOARD_VER')) {
        return '/pos';
      }

      if (state.matchedLocation == '/cuentas-por-cobrar' &&
          !sesion.can('CUENTAS_POR_COBRAR_VER')) {
        return '/pos';
      }

      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(
        path: '/tienda',
        builder: (context, state) => const TiendaPickerScreen(),
      ),
      GoRoute(path: '/pos', builder: (context, state) => const PosScreen()),
      GoRoute(path: '/caja', builder: (context, state) => const CajaScreen()),
      GoRoute(
        path: '/dashboard',
        builder: (context, state) => const DashboardRouterScreen(),
      ),
      GoRoute(
        path: '/cuentas-por-cobrar',
        builder: (context, state) => const CuentasPorCobrarScreen(),
      ),
      GoRoute(
        path: '/pendientes-error',
        builder: (context, state) => const PendientesErrorScreen(),
      ),
    ],
  );
});
