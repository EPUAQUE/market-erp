/// Configuración de entorno leída en tiempo de compilación vía `--dart-define`.
/// Nunca hardcodear la URL del backend — el mismo build debe ser re-targeteable
/// entre local, staging y producción sin cambiar código.
class Environment {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const Duration apiTimeout = Duration(
    milliseconds: int.fromEnvironment('API_TIMEOUT_MS', defaultValue: 15000),
  );
}
