import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../../clientes/application/clientes_provider.dart';
import '../../clientes/data/cliente.dart';
import '../../ventas/application/checkout_notifier.dart';
import '../../ventas/data/venta_api.dart';
import '../application/cuentas_por_cobrar_provider.dart';

/// Cobros sueltos: pagar (parcial o total) una cuenta por cobrar ya
/// existente, fuera del cobro automático que dispara `CheckoutNotifier` justo
/// al completar una venta. Antes de esta pantalla no existía ninguna forma
/// de hacer esto — ni online — ver CLAUDE.md.
class CuentasPorCobrarScreen extends ConsumerStatefulWidget {
  const CuentasPorCobrarScreen({super.key});

  @override
  ConsumerState<CuentasPorCobrarScreen> createState() =>
      _CuentasPorCobrarScreenState();
}

class _CuentasPorCobrarScreenState
    extends ConsumerState<CuentasPorCobrarScreen> {
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

    final cuentasAsync = ref.watch(
      cuentasPorCobrarPendientesProvider(tiendaId),
    );
    final clientesAsync = ref.watch(clientesProvider);
    final sesion = ref.watch(authNotifierProvider).value;
    final puedeCobrar = sesion?.can('CUENTAS_POR_COBRAR_COBRAR') ?? false;
    final puedeAnular = sesion?.can('CUENTAS_POR_COBRAR_ANULAR') ?? false;

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
        title: const Text('Cuentas por cobrar'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              controller: _busquedaController,
              decoration: const InputDecoration(
                hintText: 'Buscar por nombre, NIT o teléfono…',
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
                      'No hay cuentas por cobrar pendientes.',
                      style: TextStyle(color: Colors.black45),
                    ),
                  );
                }
                final clientes = {
                  for (final c in clientesAsync.value ?? const <Cliente>[])
                    c.id: c,
                };
                // Sin cliente resuelto todavía (lista aún cargando): se
                // muestra igual en vez de ocultarse por una búsqueda activa —
                // desaparecer una cuenta real por una carencia temporal de
                // datos sería peor que un filtro momentáneamente de más.
                final filtradas = cuentas.where((c) {
                  final cliente = clientes[c.clienteId];
                  return cliente?.coincideBusqueda(_busqueda) ?? true;
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
                    final cliente = clientes[cuenta.clienteId];
                    return _CuentaCard(
                      tiendaId: tiendaId,
                      cuenta: cuenta,
                      nombreCliente:
                          cliente?.nombre ?? 'Cliente #${cuenta.clienteId}',
                      puedeCobrar: puedeCobrar,
                      puedeAnular: puedeAnular,
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => Center(
                child: Text(
                  'No se pudieron cargar las cuentas por cobrar: $error',
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
    required this.nombreCliente,
    required this.puedeCobrar,
    required this.puedeAnular,
  });

  final int tiendaId;
  final CuentaPorCobrar cuenta;
  final String nombreCliente;
  final bool puedeCobrar;
  final bool puedeAnular;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colors = AppColors.of(context);
    return Card(
      child: ListTile(
        title: Text(nombreCliente),
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
                    color: colors.primary,
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
        onTap: puedeCobrar
            ? () => showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                builder: (_) => RegistrarAbonoSheet(
                  tiendaId: tiendaId,
                  cuenta: cuenta,
                  nombreCliente: nombreCliente,
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
        title: const Text('Anular cuenta por cobrar'),
        content: Text(
          '¿Anular la deuda de $nombreCliente por Q ${cuenta.saldoPendiente}? '
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
          .read(cuentaPorCobrarApiProvider)
          .anular(tiendaId: tiendaId, cuentaId: cuenta.id);
      ref.invalidate(cuentasPorCobrarPendientesProvider(tiendaId));
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'No se pudo anular — puede que ya tenga abonos registrados.',
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

const _metodosAbono = [
  MetodoPago.efectivo,
  MetodoPago.tarjeta,
  MetodoPago.transferencia,
];

/// Un solo canal por abono — a diferencia de `CobroSheet`, esta pantalla no
/// necesita "Crédito" (la cuenta ya ES la deuda) ni "Mixto" (nada impide
/// registrar dos abonos seguidos, uno por canal, si el pago llegó dividido).
class RegistrarAbonoSheet extends ConsumerStatefulWidget {
  const RegistrarAbonoSheet({
    super.key,
    required this.tiendaId,
    required this.cuenta,
    required this.nombreCliente,
  });

  final int tiendaId;
  final CuentaPorCobrar cuenta;
  final String nombreCliente;

  @override
  ConsumerState<RegistrarAbonoSheet> createState() =>
      _RegistrarAbonoSheetState();
}

class _RegistrarAbonoSheetState extends ConsumerState<RegistrarAbonoSheet> {
  MetodoPago _metodo = MetodoPago.efectivo;
  final _montoController = TextEditingController();
  bool _guardando = false;
  String? _error;

  @override
  void dispose() {
    _montoController.dispose();
    super.dispose();
  }

  Decimal? get _monto => Decimal.tryParse(_montoController.text.trim());

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
          .read(cuentaPorCobrarApiProvider)
          .registrarCobro(
            tiendaId: widget.tiendaId,
            cuentaId: widget.cuenta.id,
            monto: monto,
            metodoPago: _metodo,
          );
      ref.invalidate(cuentasPorCobrarPendientesProvider(widget.tiendaId));
      if (mounted) Navigator.of(context).pop();
    } catch (_) {
      setState(() {
        _guardando = false;
        _error = 'No se pudo registrar el abono.';
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
            'Abono — ${widget.nombreCliente}',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            'Saldo pendiente: Q ${widget.cuenta.saldoPendiente}',
            style: const TextStyle(color: Colors.black54),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final metodo in _metodosAbono)
                ChoiceChip(
                  label: Text(_labelMetodo(metodo)),
                  selected: _metodo == metodo,
                  selectedColor: colors.primary.withValues(alpha: 0.15),
                  onSelected: (_) => setState(() => _metodo = metodo),
                ),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _montoController,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Monto del abono',
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
                backgroundColor: colors.primary,
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
                  : const Text('REGISTRAR ABONO'),
            ),
          ),
        ],
      ),
    );
  }

  String _labelMetodo(MetodoPago metodo) => switch (metodo) {
    MetodoPago.efectivo => 'Efectivo',
    MetodoPago.tarjeta => 'Tarjeta',
    MetodoPago.transferencia => 'Transferencia',
    MetodoPago.credito => 'Crédito',
    MetodoPago.mixto => 'Mixto',
  };
}
