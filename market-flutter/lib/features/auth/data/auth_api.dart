import '../../../core/network/api_client.dart';
import 'sesion_usuario.dart';

/// Cliente delgado de `/api/v1/auth/*` — sin estado, sin Riverpod. El estado
/// vive en `AuthNotifier` (application/).
class AuthApi {
  AuthApi(this._client);

  final ApiClient _client;

  Future<String> login(String username, String password) {
    return _client.post<String>(
      '/api/v1/auth/login',
      data: {'username': username, 'password': password},
      parser: (data) => (data as Map<String, dynamic>)['accessToken'] as String,
      requiresAuth: false,
    );
  }

  Future<SesionUsuario> me() {
    return _client.get<SesionUsuario>(
      '/api/v1/auth/me',
      parser: (data) => SesionUsuario.fromJson(data as Map<String, dynamic>),
    );
  }

  Future<void> logout() {
    return _client.post<void>('/api/v1/auth/logout', parser: (_) {});
  }

  /// Siempre resuelve sin error si el request llega al backend — este nunca
  /// distingue usuario inexistente/sin correo/inactivo (mismo criterio que
  /// `AuthController.forgotPassword`), para no filtrar qué usuarios existen.
  Future<void> forgotPassword(String username) {
    return _client.post<void>(
      '/api/v1/auth/forgot-password',
      data: {'username': username},
      parser: (_) {},
      requiresAuth: false,
    );
  }

  Future<void> resetPassword(String token, String nuevaPassword) {
    return _client.post<void>(
      '/api/v1/auth/reset-password',
      data: {'token': token, 'nuevaPassword': nuevaPassword},
      parser: (_) {},
      requiresAuth: false,
    );
  }
}
