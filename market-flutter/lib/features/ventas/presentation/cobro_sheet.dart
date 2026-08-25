import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../clientes/data/cliente.dart';
import '../../clientes/presentation/cliente_selector_sheet.dart';
import '../application/checkout_notifier.dart';
import '../data/venta_api.dart';
import '../domain/carrito.dart';

const _primary = Color(0xFF2E8B57);
const _danger = Color(0xFFDC6B6B);

class CobroSheet extends ConsumerStatefulWidget {
  const CobroSheet({super.key, required this.tiendaId, required this.total});

  final int tiendaId;
  final Decimal total;

  @override
  ConsumerState<CobroSheet> createState() => _CobroSheetState();
}

const _metodosMixto = [
  MetodoPago.efectivo,
  MetodoPago.tarjeta,
  MetodoPago.transferencia,
];

class _CobroSheetState extends ConsumerState<CobroSheet> {
  MetodoPago _metodo = MetodoPago.efectivo;
  Cliente? _clienteCredito;
  final _montoController = TextEditingController();
  final _montoMixtoControllers = {
    for (final m in _metodosMixto) m: TextEditingController(),
  };

  @override
  void dispose() {
    _montoController.dispose();
    for (final c in _montoMixtoControllers.values) {
      c.dispose();
    }
    super.dispose();
  }

  Decimal? get _montoRecibido => Decimal.tryParse(_montoController.text.trim());

  Map<MetodoPago, Decimal> get _desgloseMixto => {
    for (final entry in _montoMixtoControllers.entries)
      entry.key: Decimal.tryParse(entry.value.text.trim()) ?? Decimal.zero,
  };

  Decimal get _sumaMixto =>
      _desgloseMixto.values.fold(Decimal.zero, (a, b) => a + b);

  bool get _requiereMonto => _metodo == MetodoPago.efectivo;

  bool get _puedeConfirmar {
    if (_metodo == MetodoPago.credito) return _clienteCredito != null;
    if (_metodo == MetodoPago.mixto) return _sumaMixto == widget.total;
    if (_requiereMonto) {
      final monto = _montoRecibido;
      return monto != null && monto >= widget.total;
    }
    return true;
  }

  Future<void> _elegirCliente() async {
    final cliente = await showModalBottomSheet<Cliente>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const ClienteSelectorSheet(),
    );
    if (cliente != null) setState(() => _clienteCredito = cliente);
  }

  Future<void> _confirmar() async {
    await ref
        .read(checkoutProvider.notifier)
        .confirmar(
          tiendaId: widget.tiendaId,
          metodo: _metodo,
          clienteId: _clienteCredito?.id,
          desglose: _metodo == MetodoPago.mixto ? _desgloseMixto : null,
        );
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(checkoutProvider, (previous, next) {
      if (next.ventaCompletada) {
        ref.read(checkoutProvider.notifier).reset();
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Venta completada.'),
            backgroundColor: _primary,
          ),
        );
      }
    });

    final checkout = ref.watch(checkoutProvider);
    final cambio = _metodo == MetodoPago.efectivo && _montoRecibido != null
        ? calcularCambio(total: widget.total, montoRecibido: _montoRecibido!)
        : null;

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
            'Total a cobrar   Q ${widget.total}',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _MetodoChip(
                label: 'Efectivo',
                metodo: MetodoPago.efectivo,
                seleccionado: _metodo,
                onTap: _cambiarMetodo,
              ),
              _MetodoChip(
                label: 'Tarjeta',
                metodo: MetodoPago.tarjeta,
                seleccionado: _metodo,
                onTap: _cambiarMetodo,
              ),
              _MetodoChip(
                label: 'Transferencia',
                metodo: MetodoPago.transferencia,
                seleccionado: _metodo,
                onTap: _cambiarMetodo,
              ),
              _MetodoChip(
                label: 'Crédito',
                metodo: MetodoPago.credito,
                seleccionado: _metodo,
                onTap: _cambiarMetodo,
              ),
              _MetodoChip(
                label: 'Mixto',
                metodo: MetodoPago.mixto,
                seleccionado: _metodo,
                onTap: _cambiarMetodo,
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_requiereMonto) ...[
            TextField(
              controller: _montoController,
              autofocus: true,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Monto recibido',
                prefixText: 'Q ',
                border: OutlineInputBorder(),
              ),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 8),
            Text(
              cambio != null ? 'Cambio: Q $cambio' : 'Cambio: —',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
            ),
          ],
          if (_metodo == MetodoPago.mixto) ...[
            for (final m in _metodosMixto) ...[
              TextField(
                controller: _montoMixtoControllers[m],
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: InputDecoration(
                  labelText: _labelMetodo(m),
                  prefixText: 'Q ',
                  border: const OutlineInputBorder(),
                ),
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 8),
            ],
            Text(
              'Total ingresado: Q $_sumaMixto de Q ${widget.total}',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _sumaMixto == widget.total ? _primary : _danger,
              ),
            ),
          ],
          if (_metodo == MetodoPago.credito) ...[
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(_clienteCredito?.nombre ?? 'Selecciona un cliente'),
              subtitle: _clienteCredito != null && _clienteCredito!.nit != null
                  ? Text('NIT ${_clienteCredito!.nit}')
                  : null,
              trailing: TextButton(
                onPressed: _elegirCliente,
                child: const Text('Buscar'),
              ),
            ),
            if (_clienteCredito != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  _clienteCredito!.limiteCredito != null
                      ? 'Límite de crédito: Q ${_clienteCredito!.limiteCredito}'
                      : 'Sin límite de crédito definido para este cliente.',
                  style: const TextStyle(fontSize: 12, color: Colors.black54),
                ),
              ),
          ],
          if (checkout.error != null) ...[
            const SizedBox(height: 8),
            Text(checkout.error!, style: const TextStyle(color: _danger)),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
              onPressed: checkout.loading || !_puedeConfirmar
                  ? null
                  : _confirmar,
              child: checkout.loading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text(
                      _metodo == MetodoPago.credito
                          ? 'CONFIRMAR A CRÉDITO'
                          : 'CONFIRMAR COBRO',
                    ),
            ),
          ),
        ],
      ),
    );
  }

  void _cambiarMetodo(MetodoPago metodo) => setState(() => _metodo = metodo);

  String _labelMetodo(MetodoPago metodo) => switch (metodo) {
    MetodoPago.efectivo => 'Efectivo',
    MetodoPago.tarjeta => 'Tarjeta',
    MetodoPago.transferencia => 'Transferencia',
    MetodoPago.credito => 'Crédito',
    MetodoPago.mixto => 'Mixto',
  };
}

class _MetodoChip extends StatelessWidget {
  const _MetodoChip({
    required this.label,
    required this.metodo,
    required this.seleccionado,
    required this.onTap,
  });

  final String label;
  final MetodoPago metodo;
  final MetodoPago seleccionado;
  final void Function(MetodoPago) onTap;

  @override
  Widget build(BuildContext context) {
    final activo = metodo == seleccionado;
    return ChoiceChip(
      label: Text(label),
      selected: activo,
      selectedColor: _primary.withValues(alpha: 0.15),
      onSelected: (_) => onTap(metodo),
    );
  }
}
