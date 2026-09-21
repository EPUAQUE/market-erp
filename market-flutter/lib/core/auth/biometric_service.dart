import 'package:flutter/foundation.dart';
import 'package:local_auth/local_auth.dart';

/// Wrapper delgado sobre `local_auth` — huella/Face ID vía Keystore/Keychain
/// nativo, nunca lee datos biométricos crudos. Sin soporte en web (el plugin
/// no tiene implementación ahí), así que [disponible] corta antes de tocarlo
/// para no lanzar `MissingPluginException`.
class BiometricService {
  BiometricService._();
  static final BiometricService instance = BiometricService._();

  final _auth = LocalAuthentication();

  Future<bool> disponible() async {
    if (kIsWeb) return false;
    try {
      final soportado = await _auth.isDeviceSupported();
      final puedeChequear = await _auth.canCheckBiometrics;
      return soportado && puedeChequear;
    } catch (_) {
      return false;
    }
  }

  /// `true` solo si el usuario completó la verificación biométrica real —
  /// nunca autentica solo porque el dispositivo la soporta.
  Future<bool> autenticar() async {
    try {
      return await _auth.authenticate(
        localizedReason: 'Confirma tu identidad para ingresar',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );
    } catch (_) {
      return false;
    }
  }
}
