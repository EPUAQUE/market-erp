import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/util/decimal_input.dart';
import '../../auth/application/auth_notifier.dart';
import '../../proveedores/application/proveedores_provider.dart';
import '../../proveedores/data/proveedor.dart';
import '../application/cuentas_por_pagar_provider.dart';
import '../data/cuenta_por_pagar_api.dart';

/// Pagos a proveedores desde el POS — solo visible para ENCARGADO_TIENDA y
/// ADMIN (gated por `CUENTAS_POR_PAGAR_VER`, ver `app_router.dart`/
/// `pos_screen.dart`); CAJERO no tiene este permiso. Antes de esta pantalla,
/// pagar una cuenta por pagar solo se podía hacer desde el backoffice web.
class CuentasPorPagarScreen extends ConsumerStatefulWidget {
  const CuentasPorPagarScreen({super.key});

  @override
  ConsumerState<CuentasPorPagarScreen> createState() =>
      _CuentasPorPagarScreenState();
}

class _CuentasPorPagarScreenState extends ConsumerState<CuentasPorPagarScreen> {
  final _busquedaController = TextEditingController();
  String _busqueda = '';

  @override
  void dispose() {
    _busquedaController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final cuentasAsync = ref.watch(cuentasPorPagarPendientesProvider(tiendaId));
    final proveedoresAsync = ref.watch(proveedoresProvider);
    final sesion = ref.watch(authNotifierProvider).value;
    final puedePagar = sesion?.can('CUENTAS_POR_PAGAR_PAGAR') ?? false;
    final puedeAnular = sesion?.can('CUENTAS_POR_PAGAR_ANULAR') ?? false;

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
        title: const Text('Cuentas por pagar'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              controller: _busquedaController,
              decoration: const InputDecoration(
                hintText: 'Buscar por nombre o NIT del proveedor…',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
                filled: true,
                fillColor: Colors.white,
              ),
              onChanged: (value) => setState(() => _busqueda = value),
            ),
          ),
          Expanded(
            child: cuentasAsync.when(
              data: (cuentas) {
                if (cuentas.isEmpty) {
                  return const Center(
                    child: Text(
                      'No hay cuentas por pagar pendientes.',
                      style: TextStyle(color: Colors.black45),
                    ),
                  );
                }
                final proveedores = {
                  for (final p in proveedoresAsync.value ?? const <Proveedor>[])
                    p.id: p,
                };
                // Mismo criterio que Cuentas por Cobrar: sin proveedor
                // resuelto todavía (lista aún cargando), se muestra igual en
                // vez de ocultarse por una búsqueda activa.
                final filtradas = cuentas.where((c) {
                  final proveedor = proveedores[c.proveedorId];
                  return proveedor?.coincideBusqueda(_busqueda) ?? true;
                }).toList();
                if (filtradas.isEmpty) {
                  return const Center(
                    child: Text(
                      'Sin resultados.',
                      style: TextStyle(color: Colors.black45),
                    ),
                  );
                }
                return ListView.separated(
                  padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                  itemCount: filtradas.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                  itemBuilder: (context, index) {
                    final cuenta = filtradas[index];
                    final proveedor = proveedores[cuenta.proveedorId];
                    return _CuentaCard(
                      tiendaId: tiendaId,
                      cuenta: cuenta,
                      nombreProveedor:
                          proveedor?.nombre ??
                          'Proveedor #${cuenta.proveedorId}',
                      puedePagar: puedePagar,
                      puedeAnular: puedeAnular,
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(
                child: Text(
                  'No se pudieron cargar las cuentas por pagar: $error',
                  style: TextStyle(color: colors.danger),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CuentaCard extends ConsumerWidget {
  const _CuentaCard({
    required this.tiendaId,
    required this.cuenta,
    required this.nombreProveedor,
    required this.puedePagar,
    required this.puedeAnular,
  });

  final int tiendaId;
  final CuentaPorPagar cuenta;
  final String nombreProveedor;
  final bool puedePagar;
  final bool puedeAnular;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    return Card(
      child: ListTile(
        title: Text(nombreProveedor),
        subtitle: Text(
          cuenta.vencida
              ? 'Vencida desde ${_formatearFecha(cuenta.fechaVencimiento)}'
              : 'Vence ${_formatearFecha(cuenta.fechaVencimiento)}',
          style: TextStyle(
            color: cuenta.vencida ? colors.danger : Colors.black54,
          ),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  'Q ${cuenta.saldoPendiente}',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: colors.danger,
                  ),
                ),
                if (cuenta.vencida)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Icon(
                      Icons.warning_amber,
                      size: 16,
                      color: colors.pending,
                    ),
                  ),
              ],
            ),
            if (puedeAnular)
              IconButton(
                icon: Icon(Icons.cancel_outlined, color: colors.danger),
                tooltip: 'Anular',
                onPressed: () => _confirmarAnular(context, ref),
              ),
          ],
        ),
        onTap: puedePagar
            ? () => showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                builder: (_) => RegistrarPagoSheet(
                  tiendaId: tiendaId,
                  cuenta: cuenta,
                  nombreProveedor: nombreProveedor,
                ),
              )
            : null,
      ),
    );
  }

  Future<void> _confirmarAnular(BuildContext context, WidgetRef ref) async {
    final colors = AppColors.of(context);
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Anular cuenta por pagar'),
        content: Text(
          '¿Anular la deuda con $nombreProveedor por Q ${cuenta.saldoPendiente}? '
          'Esta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: colors.danger),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Anular'),
          ),
        ],
      ),
    );
    if (confirmado != true) return;
    try {
      await ref
          .read(cuentaPorPagarApiProvider)
          .anular(tiendaId: tiendaId, cuentaId: cuenta.id);
      ref.invalidate(cuentasPorPagarPendientesProvider(tiendaId));
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'No se pudo anular — puede que ya tenga pagos registrados.',
            ),
            backgroundColor: colors.danger,
          ),
        );
      }
    }
  }

  String _formatearFecha(DateTime fecha) {
    final local = fecha.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/'
        '${local.year}';
  }
}

/// A diferencia de `RegistrarAbonoSheet` (Cuentas por Cobrar), no hay
/// selector de canal — `RegistrarPagoRequest` en el backend solo acepta
/// `monto`, sin `metodoPago` (ver market-backend).
class RegistrarPagoSheet extends ConsumerStatefulWidget {
  const RegistrarPagoSheet({
    super.key,
    required this.tiendaId,
    required this.cuenta,
    required this.nombreProveedor,
  });

  final int tiendaId;
  final CuentaPorPagar cuenta;
  final String nombreProveedor;

  @override
  ConsumerState<RegistrarPagoSheet> createState() => _RegistrarPagoSheetState();
}

class _RegistrarPagoSheetState extends ConsumerState<RegistrarPagoSheet> {
  final _montoController = TextEditingController();
  bool _guardando = false;
  String? _error;

  @override
  void dispose() {
    _montoController.dispose();
    super.dispose();
  }

  Decimal? get _monto => parseDecimalInput(_montoController.text);

  bool get _puedeConfirmar {
    final monto = _monto;
    return monto != null &&
        monto > Decimal.zero &&
        monto <= widget.cuenta.saldoPendiente;
  }

  Future<void> _confirmar() async {
    final monto = _monto;
    if (monto == null) return;
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      await ref
          .read(cuentaPorPagarApiProvider)
          .registrarPago(
            tiendaId: widget.tiendaId,
            cuentaId: widget.cuenta.id,
            monto: monto,
          );
      ref.invalidate(cuentasPorPagarPendientesProvider(widget.tiendaId));
      if (mounted) Navigator.of(context).pop();
    } catch (_) {
      setState(() {
        _guardando = false;
        _error = 'No se pudo registrar el pago.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Pago — ${widget.nombreProveedor}',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            'Saldo pendiente: Q ${widget.cuenta.saldoPendiente}',
            style: const TextStyle(color: Colors.black54),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _montoController,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Monto del pago',
              prefixText: 'Q ',
              border: OutlineInputBorder(),
            ),
            onChanged: (_) => setState(() {}),
          ),
          if (_error != null) ...[
            const SizedBox(height: 8),
            Text(_error!, style: TextStyle(color: colors.danger)),
          ],
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: colors.danger,
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
              onPressed: _guardando || !_puedeConfirmar ? null : _confirmar,
              child: _guardando
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text('REGISTRAR PAGO'),
            ),
          ),
        ],
      ),
    );
  }
}
