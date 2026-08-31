import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../productos/application/categorias_provider.dart';
import 'pos_colors.dart';

/// Columna fija de 160px con la lista vertical de categorías — layout de
/// tablet/desktop (`anchoAngosto == false`, ver `pos_screen.dart`). En
/// teléfono se reemplaza por `CategoriasChips` (`pos_body_telefono.dart`).
class ColumnaAccesos extends ConsumerWidget {
  const ColumnaAccesos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categoriasAsync = ref.watch(categoriasProvider);
    final seleccionada = ref.watch(categoriaSeleccionadaProvider);

    return Container(
      width: 160,
      color: const Color(0xFFF1F5F9),
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Text(
              'CATEGORÍAS',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.bold,
                color: Colors.black45,
              ),
            ),
          ),
          const SizedBox(height: 8),
          CategoriaItem(
            nombre: 'Todos',
            seleccionada: seleccionada == null,
            onTap: () => ref
                .read(categoriaSeleccionadaProvider.notifier)
                .seleccionar(null),
          ),
          Expanded(
            child: categoriasAsync.when(
              data: (categorias) => ListView(
                children: categorias
                    .map(
                      (categoria) => CategoriaItem(
                        nombre: categoria.nombre,
                        seleccionada: seleccionada == categoria.id,
                        onTap: () => ref
                            .read(categoriaSeleccionadaProvider.notifier)
                            .seleccionar(categoria.id),
                      ),
                    )
                    .toList(),
              ),
              loading: () => const Padding(
                padding: EdgeInsets.all(12),
                child: SizedBox(
                  height: 16,
                  width: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
              error: (_, _) => const Padding(
                padding: EdgeInsets.symmetric(horizontal: 12),
                child: Text(
                  'No se pudieron cargar las categorías.',
                  style: TextStyle(fontSize: 11, color: posColorDanger),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class CategoriaItem extends StatelessWidget {
  const CategoriaItem({
    super.key,
    required this.nombre,
    required this.seleccionada,
    required this.onTap,
  });

  final String nombre;
  final bool seleccionada;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        color: seleccionada ? const Color(0xFFDCEFE3) : Colors.transparent,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Text(
          nombre,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontWeight: seleccionada ? FontWeight.w700 : FontWeight.w500,
            color: seleccionada ? posColorPrimary : Colors.black87,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}
