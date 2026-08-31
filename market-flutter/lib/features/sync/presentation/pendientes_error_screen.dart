import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/sync/sync_engine.dart';

const _brand = Color(0xFF0F4C5C);
const _primary = Color(0xFF2E8B57);
const _danger = Color(0xFFDC6B6B);

/// Lista los ítems de la cola offline (ventas, clientes, movimientos de caja)
/// que el motor de sync marcó con `mensajeError` — un fallo de negocio, no de
/// red, así que nunca se reintentan solos (ver `SyncEngineNotifier`). Sin
/// esta pantalla quedaban enterrados en Isar, invisibles para el encargado
/// (ver CLAUDE.md, Fase 9 del plan del backend).
class PendientesErrorScreen extends ConsumerWidget {
  const PendientesErrorScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final itemsAsync = ref.watch(pendientesConErrorProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      appBar: AppBar(
        backgroundColor: _brand,
        foregroundColor: Colors.white,
        title: const Text('Pendientes con error'),
      ),
      body: itemsAsync.when(
        data: (items) {
          if (items.isEmpty) {
            return const Center(
              child: Text(
                'No hay ítems pendientes con error.',
                style: TextStyle(color: Colors.black45),
              ),
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.all(12),
            itemCount: items.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, index) => _ItemCard(item: items[index]),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Text(
            'No se pudo cargar la lista: $error',
            style: const TextStyle(color: _danger),
          ),
        ),
      ),
    );
  }
}

class _ItemCard extends ConsumerWidget {
  const _ItemCard({required this.item});

  final ItemPendienteConError item;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              item.titulo,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 2),
            Text(item.subtitulo, style: const TextStyle(color: Colors.black54)),
            const SizedBox(height: 6),
            Text(item.mensajeError, style: const TextStyle(color: _danger)),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => _confirmarDescartar(context, ref),
                  style: TextButton.styleFrom(foregroundColor: _danger),
                  child: const Text('DESCARTAR'),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: () =>
                      ref.read(syncEngineProvider.notifier).reintentar(item),
                  style: FilledButton.styleFrom(backgroundColor: _primary),
                  child: const Text('REINTENTAR'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmarDescartar(BuildContext context, WidgetRef ref) async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Descartar'),
        content: Text(
          '¿Descartar "${item.titulo}"? Esta acción no se puede deshacer — '
          'se pierde por completo, no vuelve a intentarse.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: _danger),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Descartar'),
          ),
        ],
      ),
    );
    if (confirmado != true) return;
    await ref.read(syncEngineProvider.notifier).descartar(item);
  }
}
