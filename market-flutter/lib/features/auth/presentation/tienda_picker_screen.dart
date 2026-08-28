import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../application/auth_notifier.dart';
import 'logout_confirm.dart';

/// Se muestra solo si el usuario tiene más de una tienda asignada. La
/// elección queda fija para toda la sesión — el POS, a diferencia del
/// backoffice, no tiene un switcher de tienda.
class TiendaPickerScreen extends ConsumerWidget {
  const TiendaPickerScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sesion = ref.watch(authNotifierProvider).value;
    final tiendaIds = sesion?.tiendaIds.toList() ?? const <int>[];

    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 380),
          child: Card(
            margin: const EdgeInsets.all(24),
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (tiendaIds.isEmpty) ...[
                    const Text(
                      'Este usuario no tiene ninguna tienda asignada.',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Contacta a un administrador para que te asigne una tienda antes de continuar.',
                      style: TextStyle(color: Colors.black54),
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                        onPressed: () =>
                            cerrarSesionConConfirmacion(context, ref),
                        child: const Text('Salir'),
                      ),
                    ),
                  ] else ...[
                    const Text(
                      '¿En qué tienda trabajas hoy?',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 16),
                    for (final tiendaId in tiendaIds)
                      ListTile(
                        // Nombre real requiere GET /tiendas (TIENDAS_VER) que un
                        // Vendedor/Encargado no necesariamente tiene — mismo
                        // fallback que el dashboard del backoffice.
                        contentPadding: EdgeInsets.zero,
                        leading: Icon(
                          ref.watch(tiendaActivaProvider) == tiendaId
                              ? Icons.radio_button_checked
                              : Icons.radio_button_unchecked,
                          color: const Color(0xFF2E8B57),
                        ),
                        title: Text('Tienda #$tiendaId'),
                        onTap: () => ref
                            .read(tiendaActivaProvider.notifier)
                            .seleccionar(tiendaId),
                      ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        style: FilledButton.styleFrom(
                          backgroundColor: const Color(0xFF2E8B57),
                        ),
                        onPressed: ref.watch(tiendaActivaProvider) == null
                            ? null
                            : () => context.go('/pos'),
                        child: const Text('Continuar'),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
