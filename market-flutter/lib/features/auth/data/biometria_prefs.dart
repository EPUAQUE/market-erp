import 'package:shared_preferences/shared_preferences.dart';

/// Bandera "usar huella la próxima vez" — independiente de
/// `_usuarioRecordadoKey` (`login_screen.dart`). Solo gatilla
/// `AuthNotifier.reanudarConHuella()` (refresh de la cookie de sesión ya
/// guardada); nunca guarda usuario, contraseña ni token.
const huellaHabilitadaKey = 'inven365-huella-habilitada';

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
