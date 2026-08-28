import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../network/api_client.dart';
import 'connectivity_provider.dart';

/// Cada cuánto se vuelve a probar si el backend responde mientras el
/// dispositivo mantiene una interfaz de red arriba — para detectar "Wi-Fi/datos
/// conectado pero backend caído o inalcanzable" sin esperar a que el usuario
/// dispare una acción manualmente (ver PLAN_MEJORAS.md, Fase 2).
const _intervaloRevalidacion = Duration(seconds: 15);

/// A diferencia de [redDisponibleProvider] (solo interfaz de red), este además
/// confirma que el backend realmente responde a un `GET /actuator/health` —
/// distingue "sin interfaz de red" de "hay red pero el backend está
/// inalcanzable", que antes se trataban como lo mismo en toda la app
/// (`CheckoutNotifier`, `CajaActionsNotifier`, `ClienteSelectorSheet`,
/// `SyncEngineNotifier` ya no leen `redDisponibleProvider` directamente, leen
/// este). Sin interfaz de red, ni se intenta la sonda — ya se sabe la
/// respuesta. Con interfaz arriba, sondea de inmediato y luego cada
/// [_intervaloRevalidacion] mientras la interfaz siga arriba; el `watch` sobre
/// `redDisponibleProvider` reinicia este stream en cuanto la interfaz cambia,
/// así que un reconectar real siempre dispara una sonda inmediata, no espera
/// al próximo tick del intervalo.
final backendAlcanzableProvider = StreamProvider<bool>((ref) async* {
  final hayInterfaz = ref.watch(redDisponibleProvider).value ?? false;
  if (!hayInterfaz) {
    yield false;
    return;
  }

  yield await ApiClient.instance.ping();
  yield* Stream.periodic(
    _intervaloRevalidacion,
  ).asyncMap((_) => ApiClient.instance.ping());
});
