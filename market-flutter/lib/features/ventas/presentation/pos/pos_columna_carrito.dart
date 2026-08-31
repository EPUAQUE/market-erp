import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../auth/application/auth_notifier.dart';
import '../../application/carrito_notifier.dart';
import '../cobro_sheet.dart';
import 'pos_colors.dart';

/// Columna fija (o contenido del bottom sheet en teléfono, ver
/// `pos_body_telefono.dart`) con la lista de líneas del carrito, el total y
/// el botón COBRAR que abre `CobroSheet`.
class ColumnaCarrito extends ConsumerWidget {
  const ColumnaCarrito({super.key, required this.modoRapido});

  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carrito = ref.watch(carritoProvider);
    final tiendaId = ref.watch(tiendaActivaProvider);

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Carrito (${carrito.lineas.length})',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
          ),
          const Divider(),
          Expanded(
            child: carrito.estaVacio
                ? const Center(
                    child: Text(
                      'El carrito está vacío.',
                      style: TextStyle(color: Colors.black45),
                    ),
                  )
                : ListView.separated(
                    itemCount: carrito.lineas.length,
                    separatorBuilder: (_, _) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final linea = carrito.lineas[index];
                      return ListTile(
                        dense: modoRapido,
                        contentPadding: EdgeInsets.zero,
                        title: Text(
                          linea.nombre,
                          style: const TextStyle(fontSize: 13),
                        ),
                        subtitle: modoRapido
                            ? null
                            : Text('Q ${linea.precioUnitario} c/u'),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(
                                Icons.remove_circle_outline,
                                size: 20,
                              ),
                              onPressed: () => ref
                                  .read(carritoProvider.notifier)
                                  .decrementar(linea.productoId),
                            ),
                            Text(linea.cantidad.toString()),
                            IconButton(
                              icon: const Icon(
                                Icons.add_circle_outline,
                                size: 20,
                              ),
                              onPressed: () => ref
                                  .read(carritoProvider.notifier)
                                  .incrementar(linea.productoId),
                            ),
                            Text(
                              'Q ${linea.subtotal}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
          ),
          const Divider(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Total',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              Text(
                'Q ${carrito.total}',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: posColorPrimary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: posColorPrimary,
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              icon: const Icon(Icons.payments),
              label: const Text('COBRAR', style: TextStyle(fontSize: 16)),
              onPressed: carrito.estaVacio || tiendaId == null
                  ? null
                  : () => showModalBottomSheet(
                      context: context,
                      isScrollControlled: true,
                      builder: (_) =>
                          CobroSheet(tiendaId: tiendaId, total: carrito.total),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}
