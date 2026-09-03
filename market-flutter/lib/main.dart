import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'router/app_router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // El POS nació tablet-first (10"-12", landscape), pero ahora también debe
  // andar bien en teléfono (ver `PosScreen`'s `anchoAngosto` breakpoint) — un
  // teléfono se sostiene en portrait de forma natural, así que ya no se fija
  // la app entera a landscape. Se excluye portraitDown (boca abajo) a
  // propósito: no aporta nada en un POS y solo genera un flip confuso si el
  // dispositivo se voltea por accidente.
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
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
      title: 'Inven365 POS',
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
