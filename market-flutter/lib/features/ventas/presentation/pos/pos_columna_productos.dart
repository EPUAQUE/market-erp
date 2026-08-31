import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/config/environment.dart';
import '../../../productos/application/catalogo_provider.dart';
import '../../../productos/data/producto_catalogo.dart';
import '../../application/carrito_notifier.dart';
import '../barcode_scanner_screen.dart';
import 'pos_colors.dart';

/// Columna central: buscador (+ escáner de código de barras) y la grilla de
/// productos filtrados por categoría/búsqueda.
class ColumnaProductos extends ConsumerStatefulWidget {
  const ColumnaProductos({
    super.key,
    required this.tiendaId,
    required this.modoRapido,
  });

  final int tiendaId;
  final bool modoRapido;

  @override
  ConsumerState<ColumnaProductos> createState() => _ColumnaProductosState();
}

class _ColumnaProductosState extends ConsumerState<ColumnaProductos> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onCodigoEscaneado(String codigo, List<ProductoCatalogo> catalogo) {
    for (final producto in catalogo) {
      if (producto.coincideCodigoExacto(codigo)) {
        if (!producto.vendible) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                '${producto.nombre} no tiene existencia disponible.',
              ),
            ),
          );
          return;
        }
        ref.read(carritoProvider.notifier).agregarProducto(producto);
        _searchController.clear();
        ref.read(busquedaProductoProvider.notifier).limpiar();
        return;
      }
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'No se encontró ningún producto con el código "$codigo".',
        ),
      ),
    );
  }

  Future<void> _abrirEscaner() async {
    // Modo Venta Rápida corta la transición de página — no es esencial y
    // cuesta ~300ms en cada escaneo durante una fila (ver CLAUDE.md).
    final Route<String> ruta = widget.modoRapido
        ? PageRouteBuilder(
            pageBuilder: (_, _, _) => const BarcodeScannerScreen(),
            transitionDuration: Duration.zero,
            reverseTransitionDuration: Duration.zero,
          )
        : MaterialPageRoute(builder: (_) => const BarcodeScannerScreen());
    final codigo = await Navigator.of(context).push<String>(ruta);
    if (codigo == null || codigo.isEmpty || !mounted) return;
    final catalogo = ref.read(catalogoProvider(widget.tiendaId)).value;
    if (catalogo != null) _onCodigoEscaneado(codigo, catalogo);
  }

  @override
  Widget build(BuildContext context) {
    final catalogoAsync = ref.watch(catalogoProvider(widget.tiendaId));

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Buscar por nombre, código o escanear…',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                icon: const Icon(Icons.qr_code_scanner),
                tooltip: 'Escanear código',
                onPressed: _abrirEscaner,
              ),
              filled: true,
              fillColor: Colors.white,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            onChanged: (value) =>
                ref.read(busquedaProductoProvider.notifier).actualizar(value),
            onSubmitted: (value) {
              final catalogo = catalogoAsync.value;
              if (catalogo != null && value.trim().isNotEmpty) {
                _onCodigoEscaneado(value.trim(), catalogo);
              }
            },
          ),
          const SizedBox(height: 12),
          Expanded(
            child: catalogoAsync.when(
              data: (_) => GridProductos(
                tiendaId: widget.tiendaId,
                modoRapido: widget.modoRapido,
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(
                child: Text(
                  'No se pudo cargar el catálogo: $error',
                  style: const TextStyle(color: posColorDanger),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class GridProductos extends ConsumerWidget {
  const GridProductos({
    super.key,
    required this.tiendaId,
    required this.modoRapido,
  });

  final int tiendaId;
  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filtrados = ref.watch(productosFiltradosProvider(tiendaId));
    return filtrados.when(
      data: (productos) {
        if (productos.isEmpty) {
          return const Center(
            child: Text(
              'No hay productos que coincidan.',
              style: TextStyle(color: Colors.black45),
            ),
          );
        }
        return LayoutBuilder(
          builder: (context, constraints) {
            // El recuento de columnas fijo (6/4) asumía siempre el ancho
            // completo de una tablet — en una pantalla más angosta (un
            // teléfono en landscape, por ejemplo) las columnas resultantes
            // quedaban tan angostas que el nombre/precio de la tarjeta ya no
            // cabían y desbordaban abajo. Con un ancho mínimo por celda, el
            // número de columnas se reduce en vez de encogerlas hasta romper
            // el contenido.
            const anchoMinimoCelda = 150.0;
            final maximo = modoRapido ? 6 : 4;
            final columnas = (constraints.maxWidth / anchoMinimoCelda)
                .floor()
                .clamp(2, maximo);
            return GridView.builder(
              // Más columnas y menos espaciado en Modo Venta Rápida — más
              // productos visibles a la vez, menos scroll durante una fila.
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columnas,
                mainAxisSpacing: modoRapido ? 8 : 12,
                crossAxisSpacing: modoRapido ? 8 : 12,
                childAspectRatio: modoRapido ? 1.1 : 0.95,
              ),
              itemCount: productos.length,
              itemBuilder: (context, index) => ProductoCard(
                producto: productos[index],
                modoRapido: modoRapido,
              ),
            );
          },
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => Center(child: Text('$error')),
    );
  }
}

class ProductoCard extends ConsumerWidget {
  const ProductoCard({
    super.key,
    required this.producto,
    required this.modoRapido,
  });

  final ProductoCatalogo producto;
  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final vendible = producto.vendible;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: vendible
            ? () => ref.read(carritoProvider.notifier).agregarProducto(producto)
            : null,
        child: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xFFE2E8F0)),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // La imagen es la info "secundaria" que Modo Venta Rápida
              // recorta primero — es lo más caro de renderizar/cargar y lo
              // menos necesario para reconocer un producto ya memorizado.
              if (!modoRapido)
                Expanded(
                  child: Container(
                    width: double.infinity,
                    decoration: BoxDecoration(
                      color: const Color(0xFFF1F5F9),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child:
                        Environment.resolverImagenUrl(producto.imagenUrl) !=
                            null
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.network(
                              Environment.resolverImagenUrl(
                                producto.imagenUrl,
                              )!,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) {
                                return const Icon(
                                  Icons.inventory_2_outlined,
                                  color: Colors.black26,
                                );
                              },
                            ),
                          )
                        : const Icon(
                            Icons.inventory_2_outlined,
                            color: Colors.black26,
                          ),
                  ),
                )
              else
                const Spacer(),
              const SizedBox(height: 6),
              Text(
                producto.nombre,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                'Q ${producto.precioVenta}',
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: posColorPrimary,
                ),
              ),
              if (!vendible)
                const Text(
                  'Sin existencia',
                  style: TextStyle(fontSize: 10, color: posColorDanger),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
