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
}
