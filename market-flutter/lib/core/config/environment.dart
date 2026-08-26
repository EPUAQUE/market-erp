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

  /// `Producto.imagenUrl` guarda una ruta relativa (`/api/v1/productos/imagenes/...`)
  /// desde que la subida de archivos reemplazó al campo de texto libre — pero
  /// productos creados antes de ese cambio pueden seguir teniendo una URL externa
  /// absoluta. Ambos casos deben renderizar, así que solo se antepone la base
  /// cuando no es ya absoluta.
  static String? resolverImagenUrl(String? imagenUrl) {
    if (imagenUrl == null || imagenUrl.isEmpty) return null;
    if (imagenUrl.startsWith('http://') || imagenUrl.startsWith('https://')) {
      return imagenUrl;
    }
    return '$apiBaseUrl$imagenUrl';
  }
}
