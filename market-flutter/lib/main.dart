import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'router/app_router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // El POS está diseñado para tablet horizontal (10"-12", ver CLAUDE.md) — el
  // layout de 3 columnas de PosScreen se rompe en portrait, así que se fija
  // la orientación en vez de hacerlo responsive a un caso de uso que no existe.
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);
  runApp(const ProviderScope(child: MarketPosApp()));
}

class MarketPosApp extends ConsumerWidget {
  const MarketPosApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    return MaterialApp.router(
      title: 'Market POS',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF2E8B57),
        scaffoldBackgroundColor: const Color(0xFFF8FAFC),
      ),
      routerConfig: router,
    );
  }
}
