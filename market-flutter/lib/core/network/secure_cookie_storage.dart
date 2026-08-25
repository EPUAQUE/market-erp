import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Respalda las cookies de [PersistCookieJar] en el almacén seguro del SO
/// (Android Keystore / iOS Keychain) en vez del `FileStorage` por defecto de
/// `cookie_jar` (un archivo plano en el sandbox de la app) — más apropiado
/// para un refresh token de larga vida. Las claves que usa `PersistCookieJar`
/// (`.index`, `.domains`, el host) son strings simples, sin restricciones de
/// nombre de archivo que respetar acá.
class SecureCookieStorage implements Storage {
  SecureCookieStorage({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  static const _prefix = 'cookie_jar.';

  final FlutterSecureStorage _storage;

  @override
  Future<void> init(bool persistSession, bool ignoreExpires) async {}

  @override
  Future<String?> read(String key) => _storage.read(key: '$_prefix$key');

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: '$_prefix$key', value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: '$_prefix$key');

  @override
  Future<void> deleteAll(List<String> keys) async {
    for (final key in keys) {
      await delete(key);
    }
  }
}
