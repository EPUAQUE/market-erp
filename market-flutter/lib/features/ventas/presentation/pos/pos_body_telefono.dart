import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../auth/application/auth_notifier.dart';
import '../../../productos/application/categorias_provider.dart';
import '../../application/carrito_notifier.dart';
import 'pos_colors.dart';
import 'pos_columna_carrito.dart';
import 'pos_columna_productos.dart';

// Debajo de los 700px de `anchoAngosto` (ver pos_screen.dart) las columnas
// fijas de ColumnaAccesos (160px) + ColumnaCarrito (340px) no dejan espacio
// real para el catálogo en un teléfono (~360-411dp de ancho lógico) — no es
// un desborde puntual como los de AppBar/grid, es que el layout de 3
// columnas no cabe en absoluto. Este layout reemplaza las columnas fijas por
// una sola columna: categorías como fila de chips horizontal, catálogo a
// ancho completo, carrito colapsado a una barra inferior que abre el mismo
// ColumnaCarrito en un bottom sheet.
class PosBodyTelefono extends ConsumerWidget {
  const PosBodyTelefono({
    super.key,
    required this.tiendaId,
    required this.modoRapido,
  });

  final int tiendaId;
  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      children: [
        if (!modoRapido) const CategoriasChips(),
        Expanded(
          child: ColumnaProductos(tiendaId: tiendaId, modoRapido: modoRapido),
        ),
        BarraCarritoInferior(modoRapido: modoRapido),
      ],
    );
  }
}

class CategoriasChips extends ConsumerWidget {
  const CategoriasChips({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categoriasAsync = ref.watch(categoriasProvider);
    final seleccionada = ref.watch(categoriaSeleccionadaProvider);

    return Container(
      height: 52,
      color: const Color(0xFFF1F5F9),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      child: categoriasAsync.when(
        data: (categorias) => ListView(
          scrollDirection: Axis.horizontal,
          children: [
            CategoriaChip(
              nombre: 'Todos',
              seleccionada: seleccionada == null,
              onTap: () => ref
                  .read(categoriaSeleccionadaProvider.notifier)
                  .seleccionar(null),
            ),
            for (final categoria in categorias)
              CategoriaChip(
                nombre: categoria.nombre,
                seleccionada: seleccionada == categoria.id,
                onTap: () => ref
                    .read(categoriaSeleccionadaProvider.notifier)
                    .seleccionar(categoria.id),
              ),
          ],
        ),
        loading: () => const Center(
          child: SizedBox(
            height: 16,
            width: 16,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
        error: (_, _) => const Center(
          child: Text(
            'No se pudieron cargar las categorías.',
            style: TextStyle(fontSize: 11, color: posColorDanger),
          ),
        ),
      ),
    );
  }
}

class CategoriaChip extends StatelessWidget {
  const CategoriaChip({
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
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: ChoiceChip(
        label: Text(nombre),
        selected: seleccionada,
        onSelected: (_) => onTap(),
        selectedColor: const Color(0xFFDCEFE3),
        labelStyle: TextStyle(
          fontWeight: seleccionada ? FontWeight.w700 : FontWeight.w500,
          color: seleccionada ? posColorPrimary : Colors.black87,
        ),
      ),
    );
  }
}

// Barra fija que reemplaza la columna de carrito en teléfono — muestra el
// resumen (cantidad + total) siempre visible y abre el mismo ColumnaCarrito
// (lista completa + botón COBRAR) en un bottom sheet al tocarla, en vez de
// reservarle 340px permanentes que en un teléfono no sobran.
class BarraCarritoInferior extends ConsumerWidget {
  const BarraCarritoInferior({super.key, required this.modoRapido});

  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final carrito = ref.watch(carritoProvider);
    final tiendaId = ref.watch(tiendaActivaProvider);

    void abrirCarrito() {
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        // El drag-to-dismiss del sheet es difícil de acertar en un teléfono
        // real (hay que arrastrar justo desde el borde) — un botón de cerrar
        // explícito es la vía obvia, sin depender de encontrar el gesto.
        builder: (sheetContext) => FractionallySizedBox(
          heightFactor: 0.85,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 8, 8, 0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.close),
                      tooltip: 'Cerrar carrito',
                      onPressed: () => Navigator.of(sheetContext).pop(),
                    ),
                  ],
                ),
              ),
              Expanded(child: ColumnaCarrito(modoRapido: modoRapido)),
            ],
          ),
        ),
      );
    }

    return Material(
      color: Colors.white,
      elevation: 8,
      child: InkWell(
        onTap: tiendaId == null ? null : abrirCarrito,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              const Icon(Icons.shopping_cart, color: posColorPrimary),
              const SizedBox(width: 10),
              Text(
                'Carrito (${carrito.lineas.length})',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              Text(
                'Q ${carrito.total}',
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  color: posColorPrimary,
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.keyboard_arrow_up, color: Colors.black45),
            ],
          ),
        ),
      ),
    );
  }
}
