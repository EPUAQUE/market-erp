import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'local_store.dart';
import 'local_store_selector.dart';

/// Se abre una sola vez y se reusa — `crearLocalStore()` resuelve a la
/// implementación real (Isar) o al stub de web según la plataforma, elegida
/// en tiempo de compilación por `local_store_selector.dart`.
final localStoreProvider = FutureProvider<LocalStore>(
  (ref) => crearLocalStore(),
);
