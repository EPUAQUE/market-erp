import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/db/local_store_provider.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/token_service.dart';
import '../data/auth_api.dart';
import '../data/biometria_prefs.dart';
import '../data/sesion_usuario.dart';

final authApiProvider = Provider<AuthApi>((ref) => AuthApi(ApiClient.instance));

/// `null` = no autenticado. El acceso token vive solo en [TokenService]
/// (memoria) — este notifier solo orquesta login/logout/refresh de sesión.
class AuthNotifier extends AsyncNotifier<SesionUsuario?> {
  @override
  Future<SesionUsuario?> build() async {
    ApiClient.instance.onUnauthorized = () {
      state = const AsyncData(null);
      ref.read(tiendaActivaProvider.notifier).seleccionar(null);
    };
    return null;
  }

  Future<void> login(String username, String password) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final api = ref.read(authApiProvider);
      final token = await api.login(username, password);
      TokenService.instance.set(token);
      final sesion = await api.me();
      // Con una sola tienda asignada no tiene sentido pedir que la elija —
      // se salta el TiendaPickerScreen directo.
      if (sesion.tiendaIds.length == 1) {
        ref
            .read(tiendaActivaProvider.notifier)
            .seleccionar(sesion.tiendaIds.first);
      }
      return sesion;
    });
  }

  /// Reanuda la sesión gatillado tras una verificación biométrica exitosa
  /// (`LoginScreen`). Primero intenta la cookie de refresh (rápido, no
  /// requiere red al backend de login); si ya expiró o nunca existió, cae a
  /// la contraseña guardada en el almacén seguro (`biometria_prefs.dart`) y
  /// hace un login real con ella — así la huella sigue funcionando aunque el
  /// refresh token haya vencido, pedido explícito del cliente. `false` si
  /// tampoco hay contraseña guardada o esta ya no es válida (p. ej. la
  /// cambiaron); quien llama cae de vuelta al formulario normal en ese caso.
  Future<bool> reanudarConHuella() async {
    state = const AsyncLoading();
    final refrescado = await ApiClient.instance.refrescarSesion();
    if (refrescado) {
      state = await AsyncValue.guard(() async {
        final api = ref.read(authApiProvider);
        final sesion = await api.me();
        if (sesion.tiendaIds.length == 1) {
          ref
              .read(tiendaActivaProvider.notifier)
              .seleccionar(sesion.tiendaIds.first);
        }
        return sesion;
      });
      if (!state.hasError) return true;
    }

    final credencial = await leerCredencialHuella();
    if (credencial == null) {
      state = const AsyncData(null);
      return false;
    }
    state = await AsyncValue.guard(() async {
      final api = ref.read(authApiProvider);
      final token = await api.login(credencial.usuario, credencial.password);
      TokenService.instance.set(token);
      final sesion = await api.me();
      if (sesion.tiendaIds.length == 1) {
        ref
            .read(tiendaActivaProvider.notifier)
            .seleccionar(sesion.tiendaIds.first);
      }
      return sesion;
    });
    if (state.hasError) {
      // Contraseña guardada ya no sirve (la cambiaron, cuenta bloqueada,
      // etc.) — no insistir con ella en el próximo intento.
      await borrarCredencialHuella();
      await guardarHuellaHabilitada(false);
      return false;
    }
    return true;
  }

  /// No limpia sola sin más: quien la llama (`cerrarSesionConConfirmacion`)
  /// ya confirmó que no hay pendientes sin sincronizar, o que el usuario
  /// aceptó perderlos — de lo contrario un logout borraría ventas/
  /// movimientos/clientes offline reales, no solo el mirror de catálogo.
  Future<void> logout() async {
    final api = ref.read(authApiProvider);
    try {
      await api.logout();
    } finally {
      TokenService.instance.clear();
      await ApiClient.instance.clearCookies();
      // Tablet compartida entre vendedores: no dejar el botón de huella
      // ofreciendo reanudar la sesión de quien acaba de cerrarla, ni la
      // contraseña guardada disponible para el siguiente que tome el tablet.
      await guardarHuellaHabilitada(false);
      await borrarCredencialHuella();
      final store = await ref.read(localStoreProvider.future);
      await store.limpiarTodo();
      ref.read(tiendaActivaProvider.notifier).seleccionar(null);
      state = const AsyncData(null);
    }
  }
}

final authNotifierProvider =
    AsyncNotifierProvider<AuthNotifier, SesionUsuario?>(AuthNotifier.new);

/// Tienda elegida al login — se mantiene fija durante toda la sesión, a
/// diferencia del backoffice no hay switcher.
class TiendaActivaNotifier extends Notifier<int?> {
  @override
  int? build() => null;

  void seleccionar(int? tiendaId) => state = tiendaId;
}

final tiendaActivaProvider = NotifierProvider<TiendaActivaNotifier, int?>(
  TiendaActivaNotifier.new,
);
