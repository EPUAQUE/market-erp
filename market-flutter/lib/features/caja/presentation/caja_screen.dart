import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../auth/application/auth_notifier.dart';
import '../application/caja_provider.dart';
import '../data/caja.dart';

class CajaScreen extends ConsumerWidget {
  const CajaScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tiendaId = ref.watch(tiendaActivaProvider);
    if (tiendaId == null) return const SizedBox.shrink();

    final cajaAsync = ref.watch(cajaAbiertaProvider(tiendaId));
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.bg,
      appBar: AppBar(
        backgroundColor: colors.brand,
        foregroundColor: Colors.white,
        title: const Text('Caja'),
      ),
      body: cajaAsync.when(
        data: (sesion) => sesion == null
            ? _AbrirCajaForm(tiendaId: tiendaId)
            : _CajaAbiertaView(tiendaId: tiendaId, sesion: sesion),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) =>
            Center(child: Text('No se pudo cargar la caja: $error')),
      ),
    );
  }
}

class _AbrirCajaForm extends ConsumerStatefulWidget {
  const _AbrirCajaForm({required this.tiendaId});

  final int tiendaId;

  @override
  ConsumerState<_AbrirCajaForm> createState() => _AbrirCajaFormState();
}

class _AbrirCajaFormState extends ConsumerState<_AbrirCajaForm> {
  final _montoController = TextEditingController(text: '0');

  @override
  void dispose() {
    _montoController.dispose();
    super.dispose();
  }

  Future<void> _abrir() async {
    final monto = Decimal.tryParse(_montoController.text.trim());
    if (monto == null || monto < Decimal.zero) return;
    await ref
        .read(cajaActionsProvider.notifier)
        .abrir(tiendaId: widget.tiendaId, montoInicial: monto);
  }

  @override
  Widget build(BuildContext context) {
    final actions = ref.watch(cajaActionsProvider);
    final colors = AppColors.of(context);
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 380),
        child: Card(
          margin: const EdgeInsets.all(24),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Abrir caja',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _montoController,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(
                    labelText: 'Monto inicial',
                    prefixText: 'Q ',
                    border: OutlineInputBorder(),
                  ),
                ),
                if (actions.error != null) ...[
                  const SizedBox(height: 8),
                  Text(actions.error!, style: TextStyle(color: colors.danger)),
                ],
                const SizedBox(height: 16),
                FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: colors.primary,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  onPressed: actions.loading ? null : _abrir,
                  child: actions.loading
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text('Abrir turno'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _CajaAbiertaView extends ConsumerWidget {
  const _CajaAbiertaView({required this.tiendaId, required this.sesion});

  final int tiendaId;
  final CajaSesion sesion;

  Future<void> _registrarMovimiento(
    BuildContext context,
    WidgetRef ref,
    TipoMovimientoCaja tipo,
  ) async {
    final resultado = await showDialog<(String, Decimal)>(
      context: context,
      builder: (_) => _MovimientoDialog(tipo: tipo),
    );
    if (resultado == null) return;
    await ref
        .read(cajaActionsProvider.notifier)
        .registrarMovimiento(
          tiendaId: tiendaId,
          tipo: tipo,
          concepto: resultado.$1,
          monto: resultado.$2,
        );
  }

  Future<void> _cerrar(BuildContext context, WidgetRef ref) async {
    final monto = await showDialog<Decimal>(
      context: context,
      builder: (_) => _CerrarCajaDialog(saldoEsperado: sesion.saldoEsperado),
    );
    if (monto == null) return;
    await ref
        .read(cajaActionsProvider.notifier)
        .cerrar(tiendaId: tiendaId, montoFinalContado: monto);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final actions = ref.watch(cajaActionsProvider);
    final colors = AppColors.of(context);
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Saldo esperado',
                        style: TextStyle(color: Colors.black54),
                      ),
                      Text(
                        'Q ${sesion.saldoEsperado}',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          color: colors.primary,
                        ),
                      ),
                    ],
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      const Text(
                        'Monto inicial',
                        style: TextStyle(color: Colors.black54),
                      ),
                      Text('Q ${sesion.montoInicial}'),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  icon: Icon(Icons.add_circle_outline, color: colors.primary),
                  label: const Text('Ingreso'),
                  onPressed: actions.loading
                      ? null
                      : () => _registrarMovimiento(
                          context,
                          ref,
                          TipoMovimientoCaja.ingreso,
                        ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  icon: Icon(Icons.remove_circle_outline, color: colors.danger),
                  label: const Text('Egreso'),
                  onPressed: actions.loading
                      ? null
                      : () => _registrarMovimiento(
                          context,
                          ref,
                          TipoMovimientoCaja.egreso,
                        ),
                ),
              ),
            ],
          ),
          if (actions.error != null) ...[
            const SizedBox(height: 8),
            Text(actions.error!, style: TextStyle(color: colors.danger)),
          ],
          const SizedBox(height: 16),
          const Text(
            'Movimientos',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          const Divider(),
          Expanded(
            child: sesion.movimientos.isEmpty
                ? const Center(
                    child: Text(
                      'Sin movimientos todavía.',
                      style: TextStyle(color: Colors.black45),
                    ),
                  )
                : ListView.separated(
                    itemCount: sesion.movimientos.length,
                    separatorBuilder: (_, _) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final itemColors = AppColors.of(context);
                      final movimiento = sesion.movimientos[index];
                      final esIngreso =
                          movimiento.tipo == TipoMovimientoCaja.ingreso;
                      return ListTile(
                        leading: Icon(
                          esIngreso ? Icons.add_circle : Icons.remove_circle,
                          color: esIngreso
                              ? itemColors.primary
                              : itemColors.danger,
                        ),
                        title: Text(movimiento.concepto),
                        subtitle: Text(movimiento.fecha.toLocal().toString()),
                        trailing: Text(
                          '${esIngreso ? '+' : '-'} Q ${movimiento.monto}',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: esIngreso
                                ? itemColors.primary
                                : itemColors.danger,
                          ),
                        ),
                      );
                    },
                  ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              style: OutlinedButton.styleFrom(
                foregroundColor: colors.danger,
                side: BorderSide(color: colors.danger),
              ),
              onPressed: actions.loading ? null : () => _cerrar(context, ref),
              child: const Text('Cerrar caja'),
            ),
          ),
        ],
      ),
    );
  }
}

class _MovimientoDialog extends StatefulWidget {
  const _MovimientoDialog({required this.tipo});

  final TipoMovimientoCaja tipo;

  @override
  State<_MovimientoDialog> createState() => _MovimientoDialogState();
}

class _MovimientoDialogState extends State<_MovimientoDialog> {
  final _conceptoController = TextEditingController();
  final _montoController = TextEditingController();

  @override
  void dispose() {
    _conceptoController.dispose();
    _montoController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final esIngreso = widget.tipo == TipoMovimientoCaja.ingreso;
    final colors = AppColors.of(context);
    return AlertDialog(
      title: Text(esIngreso ? 'Registrar ingreso' : 'Registrar egreso'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _conceptoController,
            decoration: const InputDecoration(
              labelText: 'Concepto',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _montoController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Monto',
              prefixText: 'Q ',
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(
            backgroundColor: esIngreso ? colors.primary : colors.danger,
          ),
          onPressed: () {
            final concepto = _conceptoController.text.trim();
            final monto = Decimal.tryParse(_montoController.text.trim());
            if (concepto.isEmpty || monto == null || monto <= Decimal.zero) {
              return;
            }
            Navigator.of(context).pop((concepto, monto));
          },
          child: const Text('Registrar'),
        ),
      ],
    );
  }
}

class _CerrarCajaDialog extends StatefulWidget {
  const _CerrarCajaDialog({required this.saldoEsperado});

  final Decimal saldoEsperado;

  @override
  State<_CerrarCajaDialog> createState() => _CerrarCajaDialogState();
}

class _CerrarCajaDialogState extends State<_CerrarCajaDialog> {
  final _montoController = TextEditingController();

  @override
  void dispose() {
    _montoController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final contado = Decimal.tryParse(_montoController.text.trim());
    final diferencia = contado != null ? contado - widget.saldoEsperado : null;
    final colors = AppColors.of(context);
    return AlertDialog(
      title: const Text('Cerrar caja'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Saldo esperado: Q ${widget.saldoEsperado}'),
          const SizedBox(height: 10),
          TextField(
            controller: _montoController,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(
              labelText: 'Monto contado',
              prefixText: 'Q ',
              border: OutlineInputBorder(),
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          if (diferencia != null)
            Text(
              'Diferencia: Q $diferencia',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                color: diferencia == Decimal.zero
                    ? colors.primary
                    : colors.danger,
              ),
            ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: colors.danger),
          onPressed: contado == null || contado < Decimal.zero
              ? null
              : () => Navigator.of(context).pop(contado),
          child: const Text('Confirmar cierre'),
        ),
      ],
    );
  }
}
