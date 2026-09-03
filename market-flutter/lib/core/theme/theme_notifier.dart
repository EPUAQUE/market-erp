import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Preferencia de tema — siempre abre en claro, cada usuario elige oscuro si
/// quiere (mismo criterio que `theme.store.ts` del backoffice: nunca sigue el
/// tema del sistema operativo). Se persiste en `SharedPreferences` bajo la
/// misma clave lógica que el backoffice usa en `localStorage`
/// (`inven365-tema`), aunque son dispositivos distintos y no comparten el
/// valor — es solo la misma convención de nombre.
class ThemeNotifier extends Notifier<ThemeMode> {
  static const _prefKey = 'inven365-tema';

  @override
  ThemeMode build() {
    _cargarGuardado();
    return ThemeMode.light;
  }

  Future<void> _cargarGuardado() async {
    final prefs = await SharedPreferences.getInstance();
    if (prefs.getString(_prefKey) == 'oscuro') {
      state = ThemeMode.dark;
    }
  }

  Future<void> alternar() async {
    state = state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      _prefKey,
      state == ThemeMode.dark ? 'oscuro' : 'claro',
    );
  }
}

final themeModeProvider = NotifierProvider<ThemeNotifier, ThemeMode>(
  ThemeNotifier.new,
);
