import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/config/environment.dart';
import '../../auth/application/auth_notifier.dart';
import '../../productos/application/catalogo_provider.dart';
import '../../productos/application/categorias_provider.dart';
import '../../productos/data/producto_catalogo.dart';
import '../../../shared/widgets/connectivity_badge.dart';
import '../application/carrito_notifier.dart';
import '../application/modo_venta_rapida_provider.dart';
import 'barcode_scanner_screen.dart';
import 'cobro_sheet.dart';

const _brand = Color(0xFF0F4C5C);
const _primary = Color(0xFF2E8B57);
const _danger = Color(0xFFDC6B6B);

class _AccionPos {
  const _AccionPos({
    required this.icono,
    required this.etiqueta,
    required this.onTap,
  });

  final IconData icono;
  final String etiqueta;
  final VoidCallback onTap;
}

class PosScreen extends ConsumerWidget {
  const PosScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final sesion = ref.watch(authNotifierProvider).value;
    final puedeVerCaja = sesion?.can('CAJA_VER') ?? false;
    final puedeVerDashboard = sesion?.can('DASHBOARD_VER') ?? false;
    final puedeVerCuentasPorCobrar =
        sesion?.can('CUENTAS_POR_COBRAR_VER') ?? false;
    final modoRapido = ref.watch(modoVentaRapidaProvider);
    // Debajo de este ancho (teléfonos en portrait, ~411dp lógicos en un
    // Pixel de gama media) el título + badge + 5 botones de acción ya no
    // caben en una sola fila del AppBar — desbordan a la derecha. Esta app
    // es tablet-first (ver CLAUDE.md), pero debe seguir siendo usable sin
    // desbordar en una pantalla más angosta.
    final anchoAngosto = MediaQuery.sizeOf(context).width < 700;

    final accionesNavegacion = <_AccionPos>[
      if (puedeVerDashboard)
        _AccionPos(
          icono: Icons.dashboard_outlined,
          etiqueta: 'Dashboard',
          onTap: () => context.push('/dashboard'),
        ),
      if (puedeVerCaja)
        _AccionPos(
          icono: Icons.point_of_sale,
          etiqueta: 'Caja',
          onTap: () => context.push('/caja'),
        ),
      if (puedeVerCuentasPorCobrar)
        _AccionPos(
          icono: Icons.request_quote_outlined,
          etiqueta: 'Cuentas por cobrar',
          onTap: () => context.push('/cuentas-por-cobrar'),
        ),
      _AccionPos(
        icono: Icons.logout,
        etiqueta: 'Salir',
        onTap: () => ref.read(authNotifierProvider.notifier).logout(),
      ),
    ];

    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      appBar: AppBar(
        backgroundColor: _brand,
        foregroundColor: Colors.white,
        title: const Text('POS · Nueva Venta'),
        actions: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Center(child: ConnectivityBadge()),
          ),
          IconButton(
            icon: Icon(modoRapido ? Icons.bolt : Icons.bolt_outlined),
            color: modoRapido ? const Color(0xFFF4B942) : null,
            tooltip: modoRapido
                ? 'Salir de Modo Venta Rápida'
                : 'Modo Venta Rápida',
            onPressed: () =>
                ref.read(modoVentaRapidaProvider.notifier).alternar(),
          ),
          if (anchoAngosto)
            PopupMenuButton<VoidCallback>(
              icon: const Icon(Icons.more_vert),
              onSelected: (accion) => accion(),
              itemBuilder: (context) => [
                for (final accion in accionesNavegacion)
                  PopupMenuItem(
                    value: accion.onTap,
                    child: Row(
                      children: [
                        Icon(accion.icono, size: 20, color: Colors.black54),
                        const SizedBox(width: 12),
                        Text(accion.etiqueta),
                      ],
                    ),
                  ),
              ],
            )
          else
            for (final accion in accionesNavegacion)
              IconButton(
                icon: Icon(accion.icono),
                tooltip: accion.etiqueta,
                onPressed: accion.onTap,
              ),
        ],
      ),
      body: anchoAngosto
          ? _PosBodyTelefono(tiendaId: tiendaId, modoRapido: modoRapido)
          : Row(
              children: [
                if (!modoRapido) const _ColumnaAccesos(),
                Expanded(
                  child: _ColumnaProductos(
                    tiendaId: tiendaId,
                    modoRapido: modoRapido,
                  ),
                ),
                SizedBox(
                  width: 340,
                  child: _ColumnaCarrito(modoRapido: modoRapido),
                ),
              ],
            ),
    );
  }
}

// Debajo de los 700px de `anchoAngosto` las columnas fijas de _ColumnaAccesos
// (160px) + _ColumnaCarrito (340px) no dejan espacio real para el catálogo
// en un teléfono (~360-411dp de ancho lógico) — no es un desborde puntual
// como los de AppBar/grid, es que el layout de 3 columnas no cabe en absoluto.
// Este layout reemplaza las columnas fijas por una sola columna: categorías
// como fila de chips horizontal, catálogo a ancho completo, carrito colapsado
// a una barra inferior que abre el mismo `_ColumnaCarrito` en un bottom sheet.
class _PosBodyTelefono extends ConsumerWidget {
  const _PosBodyTelefono({required this.tiendaId, required this.modoRapido});

  final int tiendaId;
  final bool modoRapido;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      children: [
        if (!modoRapido) const _CategoriasChips(),
        Expanded(
          child: _ColumnaProductos(tiendaId: tiendaId, modoRapido: modoRapido),
        ),
        _BarraCarritoInferior(modoRapido: modoRapido),
      ],
    );
  }
}

class _CategoriasChips extends ConsumerWidget {
  const _CategoriasChips();

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
            _CategoriaChip(
              nombre: 'Todos',
              seleccionada: seleccionada == null,
              onTap: () => ref
                  .read(categoriaSeleccionadaProvider.notifier)
                  .seleccionar(null),
            ),
            for (final categoria in categorias)
              _CategoriaChip(
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
            style: TextStyle(fontSize: 11, color: _danger),
          ),
        ),
      ),
    );
  }
}

class _CategoriaChip extends StatelessWidget {
  const _CategoriaChip({
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
          color: seleccionada ? _primary : Colors.black87,
        ),
      ),
    );
  }
}

// Barra fija que reemplaza la columna de carrito en teléfono — muestra el
// resumen (cantidad + total) siempre visible y abre el mismo `_ColumnaCarrito`
// (lista completa + botón COBRAR) en un bottom sheet al tocarla, en vez de
// reservarle 340px permanentes que en un teléfono no sobran.
class _BarraCarritoInferior extends ConsumerWidget {
  const _BarraCarritoInferior({required this.modoRapido});

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
              Expanded(child: _ColumnaCarrito(modoRapido: modoRapido)),
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
              const Icon(Icons.shopping_cart, color: _primary),
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
                  color: _primary,
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

class _ColumnaAccesos extends ConsumerWidget {
  const _ColumnaAccesos();

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
          _CategoriaItem(
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
                      (categoria) => _CategoriaItem(
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
                  style: TextStyle(fontSize: 11, color: _danger),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoriaItem extends StatelessWidget {
  const _CategoriaItem({
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
            color: seleccionada ? _primary : Colors.black87,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}

class _ColumnaProductos extends ConsumerStatefulWidget {
  const _ColumnaProductos({required this.tiendaId, required this.modoRapido});

  final int tiendaId;
  final bool modoRapido;

  @override
  ConsumerState<_ColumnaProductos> createState() => _ColumnaProductosState();
}

class _ColumnaProductosState extends ConsumerState<_ColumnaProductos> {
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
              data: (_) => _GridProductos(
                tiendaId: widget.tiendaId,
                modoRapido: widget.modoRapido,
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(
                child: Text(
                  'No se pudo cargar el catálogo: $error',
                  style: const TextStyle(color: _danger),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _GridProductos extends ConsumerWidget {
  const _GridProductos({required this.tiendaId, required this.modoRapido});

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
              itemBuilder: (context, index) => _ProductoCard(
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

class _ProductoCard extends ConsumerWidget {
  const _ProductoCard({required this.producto, required this.modoRapido});

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
                    child: Environment.resolverImagenUrl(producto.imagenUrl) != null
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.network(
                              Environment.resolverImagenUrl(producto.imagenUrl)!,
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
                  color: _primary,
                ),
              ),
              if (!vendible)
                const Text(
                  'Sin existencia',
                  style: TextStyle(fontSize: 10, color: _danger),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ColumnaCarrito extends ConsumerWidget {
  const _ColumnaCarrito({required this.modoRapido});

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
                  color: _primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
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
