import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../productos/application/catalogo_provider.dart';
import '../../productos/data/producto_catalogo.dart';
import '../../ventas/presentation/barcode_scanner_screen.dart';
import '../application/inventario_ingresos_provider.dart';
import '../data/ingreso_producto.dart';

/// Consulta de los últimos ingresos (compras) de un producto en TODAS las
/// tiendas del grupo al que pertenece la tienda propia, con costo y
/// proveedor. Gateada por `INVENTARIO_INGRESOS_VER_GRUPO` (distinto de
/// `INVENTARIO_VER_GRUPO`, que solo expone existencia): decisión explícita
/// del cliente — costo y proveedor son datos sensibles, solo el encargado
/// DEL GRUPO (rol `ADMIN_GRUPO`) y el administrador global pueden verlos
/// (ver `app_router.dart`/`pos_screen.dart`).
class InventarioIngresosScreen extends ConsumerStatefulWidget {
  const InventarioIngresosScreen({super.key});

  @override
  ConsumerState<InventarioIngresosScreen> createState() =>
      _InventarioIngresosScreenState();
}

class _InventarioIngresosScreenState
    extends ConsumerState<InventarioIngresosScreen> {
  final _codigoController = TextEditingController();
  bool _buscando = false;
  String? _error;
  ProductoCatalogo? _producto;
  List<IngresoProducto>? _ingresos;

  @override
  void dispose() {
    _codigoController.dispose();
    super.dispose();
  }

  Future<void> _buscar(int tiendaId, String codigo) async {
    final codigoLimpio = codigo.trim();
    if (codigoLimpio.isEmpty) return;

    setState(() {
      _buscando = true;
      _error = null;
    });

    List<ProductoCatalogo> catalogo;
    try {
      // `.future` (no `.value` síncrono) — si el catálogo todavía está
      // cargando, esto espera a que resuelva en vez de tratarlo como lista
      // vacía y reportar "no encontrado" para un producto que sí existe
      // (mismo bug ya encontrado y corregido en InventarioGrupoScreen).
      catalogo = await ref.read(catalogoProvider(tiendaId).future);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _buscando = false;
        _error = 'No se pudo cargar el catálogo.';
      });
      return;
    }
    if (!mounted) return;

    ProductoCatalogo? producto;
    for (final p in catalogo) {
      if (p.coincideCodigoExacto(codigoLimpio)) {
        producto = p;
        break;
      }
    }
    if (producto == null) {
      setState(() {
        _buscando = false;
        _error =
            'No se encontró ningún producto con el código "$codigoLimpio".';
        _producto = null;
        _ingresos = null;
      });
      return;
    }

    setState(() {
      _producto = producto;
      _ingresos = null;
    });
    try {
      final ingresos = await ref
          .read(inventarioIngresosApiProvider)
          .obtenerPorGrupo(tiendaId: tiendaId, productoId: producto.productoId);
      if (!mounted) return;
      setState(() => _ingresos = ingresos);
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'No se pudieron cargar los ingresos del grupo.');
    } finally {
      if (mounted) setState(() => _buscando = false);
    }
  }

  Future<void> _abrirEscaner(int tiendaId) async {
    final codigo = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const BarcodeScannerScreen()),
    );
    if (codigo == null || codigo.isEmpty || !mounted) return;
    _codigoController.text = codigo;
    await _buscar(tiendaId, codigo);
  }

  String _formatearFecha(DateTime fecha) {
    final local = fecha.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/'
        '${local.year}';
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
        title: const Text('Últimos ingresos'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _codigoController,
              decoration: InputDecoration(
                hintText: 'Código interno o de barras',
                prefixIcon: const Icon(Icons.inventory_2_outlined),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.qr_code_scanner),
                  tooltip: 'Escanear código',
                  onPressed: () => _abrirEscaner(tiendaId),
                ),
                border: const OutlineInputBorder(),
              ),
              textInputAction: TextInputAction.search,
              onSubmitted: (value) => _buscar(tiendaId, value),
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                style: FilledButton.styleFrom(backgroundColor: colors.primary),
                onPressed: _buscando
                    ? null
                    : () => _buscar(tiendaId, _codigoController.text),
                child: _buscando
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('Buscar'),
              ),
            ),
            const SizedBox(height: 16),
            if (_error != null)
              Text(_error!, style: TextStyle(color: colors.danger)),
            if (_producto != null) ...[
              Text(
                _producto!.nombre,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                'Código: ${_producto!.codigoInterno}',
                style: const TextStyle(color: Colors.black54),
              ),
              const SizedBox(height: 12),
            ],
            if (_ingresos != null)
              Expanded(
                child: _ingresos!.isEmpty
                    ? const Center(
                        child: Text(
                          'No hay ingresos registrados para este producto en el grupo.',
                          style: TextStyle(color: Colors.black45),
                        ),
                      )
                    : ListView.separated(
                        itemCount: _ingresos!.length,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final ingreso = _ingresos![index];
                          final esTiendaPropia = ingreso.tiendaId == tiendaId;
                          return ListTile(
                            leading: Icon(
                              Icons.local_shipping_outlined,
                              color: esTiendaPropia ? colors.primary : null,
                            ),
                            title: Text(
                              ingreso.tiendaNombre,
                              style: TextStyle(
                                fontWeight: esTiendaPropia
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                              ),
                            ),
                            subtitle: Text(
                              '${_formatearFecha(ingreso.fecha)} · '
                              '${ingreso.proveedorNombre ?? "Proveedor desconocido"}',
                            ),
                            trailing: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  'Q ${ingreso.costoUnitario} c/u',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: colors.primary,
                                  ),
                                ),
                                Text(
                                  '${ingreso.cantidad} unid.',
                                  style: const TextStyle(
                                    color: Colors.black54,
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
              ),
          ],
        ),
      ),
    );
  }
}
