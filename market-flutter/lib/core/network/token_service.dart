/// Guarda el access token **solo en memoria** — nunca a disco. Desacoplado de
/// Riverpod/Dio a propósito (mismo motivo que `token.service.ts` en el
/// backoffice): el interceptor de Dio y el `AuthNotifier` lo comparten sin
/// crear un ciclo de importación entre `core/network` y `features/auth`.
class TokenService {
  TokenService._();
  static final TokenService instance = TokenService._();

  String? _accessToken;

  String? get accessToken => _accessToken;

  bool get hasToken => _accessToken != null;

  void set(String token) => _accessToken = token;

  void clear() => _accessToken = null;
}
