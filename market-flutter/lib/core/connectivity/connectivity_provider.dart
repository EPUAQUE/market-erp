import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Tres estados visibles en el shell (ver CLAUDE.md, "Flujos offline") — nunca
/// bloquean la interacción, solo informan. `sincronizando` lo controla
/// `SyncEngine`, no esta clase — aquí solo se distingue conectado/sin conexión.
enum EstadoConexion { conectado, sincronizando, sinConexion }

bool _hayRed(List<ConnectivityResult> resultados) {
  return resultados.any((r) => r != ConnectivityResult.none);
}

/// Stream crudo de conectividad de red (no confundir con "hay servidor
/// alcanzable" — solo dice si el dispositivo tiene una interfaz de red arriba).
final redDisponibleProvider = StreamProvider<bool>((ref) async* {
  final connectivity = Connectivity();
  yield _hayRed(await connectivity.checkConnectivity());
  yield* connectivity.onConnectivityChanged.map(_hayRed);
});
