import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../productos/application/catalogo_provider.dart';
import '../../productos/data/producto_catalogo.dart';
import '../../ventas/presentation/barcode_scanner_screen.dart';
import '../application/inventario_grupo_provider.dart';
import '../data/existencia_tienda.dart';

/// Consulta de existencias de un producto en TODAS las tiendas del grupo al
/// que pertenece la tienda propia — no solo la propia. Gateada por
/// `INVENTARIO_VER_GRUPO` (distinto de `INVENTARIO_VER`): decisión explícita
/// del cliente, el cajero no debe poder hacer esta consulta — y tampoco
/// cualquier encargado de tienda individual, solo el encargado DEL GRUPO
/// (rol `ADMIN_GRUPO`, ver `market-backend`) y el administrador global (ver
/// `app_router.dart`/`pos_screen.dart`).
class InventarioGrupoScreen extends ConsumerStatefulWidget {
  const InventarioGrupoScreen({super.key});

  @override
  ConsumerState<InventarioGrupoScreen> createState() =>
      _InventarioGrupoScreenState();
}

class _InventarioGrupoScreenState extends ConsumerState<InventarioGrupoScreen> {
  final _codigoController = TextEditingController();
  bool _buscando = false;
  String? _error;
  ProductoCatalogo? _producto;
  List<ExistenciaTienda>? _existencias;

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
      // cargando (recién se abrió esta pantalla, antes de que termine el
      // primer fetch de `catalogoProvider`), esto espera a que resuelva en
      // vez de tratarlo como lista vacía y reportar "no encontrado" para un
      // producto que sí existe.
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
        _existencias = null;
      });
      return;
    }

    setState(() {
      _producto = producto;
      _existencias = null;
    });
    try {
      final existencias = await ref
          .read(inventarioGrupoApiProvider)
          .obtenerPorGrupo(tiendaId: tiendaId, productoId: producto.productoId);
      if (!mounted) return;
      setState(() => _existencias = existencias);
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _error = 'No se pudieron cargar las existencias del grupo.',
      );
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
        title: const Text('Existencias por tienda'),
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
            if (_existencias != null)
              Expanded(
                child: _existencias!.isEmpty
                    ? const Center(
                        child: Text(
                          'La tienda no pertenece a ningún grupo con más tiendas.',
                          style: TextStyle(color: Colors.black45),
                        ),
                      )
                    : ListView.separated(
                        itemCount: _existencias!.length,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final existencia = _existencias![index];
                          final esTiendaPropia =
                              existencia.tiendaId == tiendaId;
                          return ListTile(
                            leading: Icon(
                              Icons.storefront_outlined,
                              color: esTiendaPropia ? colors.primary : null,
                            ),
                            title: Text(
                              existencia.tiendaNombre,
                              style: TextStyle(
                                fontWeight: esTiendaPropia
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                              ),
                            ),
                            subtitle: esTiendaPropia
                                ? const Text('Tu tienda')
                                : null,
                            trailing: Text(
                              '${existencia.existenciaActual}',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color:
                                    existencia.existenciaActual > Decimal.zero
                                    ? colors.primary
                                    : colors.danger,
                              ),
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
