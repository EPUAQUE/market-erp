import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Bandera "usar huella la próxima vez" — independiente de
/// `_usuarioRecordadoKey` (`login_screen.dart`). Solo gatilla
/// `AuthNotifier.reanudarConHuella()`.
const huellaHabilitadaKey = 'inven365-huella-habilitada';

const _storage = FlutterSecureStorage();
const _usuarioKey = 'inven365-huella-usuario';
const _passwordKey = 'inven365-huella-password';

Future<bool> leerHuellaHabilitada() async {
  final prefs = await SharedPreferences.getInstance();
  return prefs.getBool(huellaHabilitadaKey) ?? false;
}

Future<void> guardarHuellaHabilitada(bool habilitada) async {
  final prefs = await SharedPreferences.getInstance();
  if (habilitada) {
    await prefs.setBool(huellaHabilitadaKey, true);
  } else {
    await prefs.remove(huellaHabilitadaKey);
  }
}

class CredencialHuella {
  const CredencialHuella({required this.usuario, required this.password});

  final String usuario;
  final String password;
}

/// Guarda usuario+contraseña en el almacén seguro del SO (Android Keystore /
/// iOS Keychain), la misma capa que ya respalda la cookie de refresh
/// (`SecureCookieStorage`). Pedido explícito del cliente: login solo con
/// huella, sin depender de que el refresh token siga vigente — a cambio de
/// no cumplir ya la política previa de "nunca contraseña en disco" (ver
/// `market-flutter/CLAUDE.md`, "Login con huella digital").
Future<void> guardarCredencialHuella(String usuario, String password) async {
  await _storage.write(key: _usuarioKey, value: usuario);
  await _storage.write(key: _passwordKey, value: password);
}

Future<CredencialHuella?> leerCredencialHuella() async {
  final usuario = await _storage.read(key: _usuarioKey);
  final password = await _storage.read(key: _passwordKey);
  if (usuario == null || password == null) return null;
  return CredencialHuella(usuario: usuario, password: password);
}

Future<void> borrarCredencialHuella() async {
  await _storage.delete(key: _usuarioKey);
  await _storage.delete(key: _passwordKey);
}
