import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/connectivity/backend_reachability_provider.dart';
import '../../../core/db/local_store_provider.dart';
import '../../../core/util/correlation_id.dart';
import '../application/clientes_provider.dart';
import '../data/cliente.dart';
import '../data/cliente_pendiente_local.dart';
import '../../../core/theme/app_colors.dart';

/// Hoja de selección/alta rápida de cliente — usada por el flujo de venta a
/// crédito. Devuelve un [ClienteSeleccionado] elegido/creado vía
/// `Navigator.pop`.
class ClienteSelectorSheet extends ConsumerStatefulWidget {
  const ClienteSelectorSheet({super.key});

  @override
  ConsumerState<ClienteSelectorSheet> createState() =>
      _ClienteSelectorSheetState();
}

class _ClienteSelectorSheetState extends ConsumerState<ClienteSelectorSheet> {
  final _searchController = TextEditingController();
  String _busqueda = '';
  bool _modoCrear = false;

  final _nombreController = TextEditingController();
  final _telefonoController = TextEditingController();
  final _nitController = TextEditingController();
  final _limiteCreditoController = TextEditingController();
  bool _guardando = false;
  String? _errorCrear;

  @override
  void dispose() {
    _searchController.dispose();
    _nombreController.dispose();
    _telefonoController.dispose();
    _nitController.dispose();
    _limiteCreditoController.dispose();
    super.dispose();
  }

  Future<void> _guardarClienteNuevo() async {
    final nombre = _nombreController.text.trim();
    if (nombre.isEmpty) {
      setState(() => _errorCrear = 'El nombre es obligatorio.');
      return;
    }
    setState(() {
      _guardando = true;
      _errorCrear = null;
    });
    final telefono = _telefonoController.text.trim().isEmpty
        ? null
        : _telefonoController.text.trim();
    final nit = _nitController.text.trim().isEmpty
        ? null
        : _nitController.text.trim();
    final limiteCredito = Decimal.tryParse(
      _limiteCreditoController.text.trim(),
    );

    final correlationId = nuevoCorrelationId();
    final hayRed = ref.read(backendAlcanzableProvider).value ?? true;
    if (!hayRed) {
      await _guardarClienteNuevoOffline(
        correlationId: correlationId,
        nombre: nombre,
        telefono: telefono,
        nit: nit,
        limiteCredito: limiteCredito,
      );
      return;
    }

    try {
      final cliente = await ref
          .read(clientesApiProvider)
          .crear(
            nombre: nombre,
            telefono: telefono,
            nit: nit,
            limiteCredito: limiteCredito,
            correlationId: correlationId,
          );
      ref.invalidate(clientesProvider);
      if (mounted) {
        Navigator.of(context).pop(ClienteSeleccionado.sincronizado(cliente));
      }
    } catch (_) {
      setState(() {
        _guardando = false;
        _errorCrear = 'No se pudo crear el cliente.';
      });
    }
  }

  /// Sin conexión no se puede asignar un id real de cliente, así que el
  /// alta queda encolada para sincronizar (ver `SyncEngine`) — pero SÍ se
  /// puede usar de inmediato en la venta actual: la hoja devuelve una
  /// referencia pendiente (`ClienteSeleccionado.pendienteLocal`) con el id
  /// local recién asignado. `CheckoutNotifier` la guarda junto con la venta
  /// y `SyncEngineNotifier` la resuelve al id real una vez que este cliente
  /// sincronice (clientes siempre se drenan antes que ventas).
  Future<void> _guardarClienteNuevoOffline({
    required String correlationId,
    required String nombre,
    required String? telefono,
    required String? nit,
    required Decimal? limiteCredito,
  }) async {
    try {
      final store = await ref.read(localStoreProvider.future);
      if (!store.disponible) {
        setState(() {
          _guardando = false;
          _errorCrear =
              'Sin conexión y sin almacenamiento local disponible en este dispositivo.';
        });
        return;
      }
      final pendienteLocalId = await store.encolarClientePendiente(
        NuevoClientePendiente(
          correlationId: correlationId,
          nombre: nombre,
          telefono: telefono,
          nit: nit,
          limiteCredito: limiteCredito,
          creadaEn: DateTime.now(),
        ),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Cliente guardado — se creará al reconectar y ya puedes usarlo '
            'en esta venta.',
          ),
        ),
      );
      Navigator.of(context).pop(
        ClienteSeleccionado.pendienteLocal(
          pendienteLocalId: pendienteLocalId,
          nombre: nombre,
          nit: nit,
          limiteCredito: limiteCredito,
        ),
      );
    } catch (_) {
      setState(() {
        _guardando = false;
        _errorCrear = 'No se pudo guardar el cliente sin conexión.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: _modoCrear ? _buildFormularioCrear() : _buildBusqueda(),
    );
  }

  Widget _buildBusqueda() {
    final clientesAsync = ref.watch(clientesProvider);
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Cliente',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _searchController,
          decoration: const InputDecoration(
            hintText: 'Buscar por nombre, NIT o teléfono…',
            prefixIcon: Icon(Icons.search),
            border: OutlineInputBorder(),
          ),
          onChanged: (value) => setState(() => _busqueda = value),
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 320,
          child: clientesAsync.when(
            data: (clientes) {
              final filtrados = clientes
                  .where((c) => c.coincideBusqueda(_busqueda))
                  .toList();
              if (filtrados.isEmpty) {
                return const Center(
                  child: Text(
                    'Sin resultados.',
                    style: TextStyle(color: Colors.black45),
                  ),
                );
              }
              return ListView.builder(
                itemCount: filtrados.length,
                itemBuilder: (context, index) {
                  final cliente = filtrados[index];
                  return ListTile(
                    title: Text(cliente.nombre),
                    subtitle: Text(
                      [
                        if (cliente.nit != null) 'NIT ${cliente.nit}',
                        if (cliente.telefono != null) cliente.telefono!,
                      ].join(' · '),
                    ),
                    onTap: () => Navigator.of(
                      context,
                    ).pop(ClienteSeleccionado.sincronizado(cliente)),
                  );
                },
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (error, _) => Center(child: Text('$error')),
          ),
        ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          icon: const Icon(Icons.person_add_alt),
          label: const Text('Crear cliente nuevo'),
          onPressed: () => setState(() => _modoCrear = true),
        ),
      ],
    );
  }

  Widget _buildFormularioCrear() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Cliente nuevo',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _nombreController,
          decoration: const InputDecoration(
            labelText: 'Nombre',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _telefonoController,
          decoration: const InputDecoration(
            labelText: 'Teléfono',
            border: OutlineInputBorder(),
          ),
          keyboardType: TextInputType.phone,
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _nitController,
          decoration: const InputDecoration(
            labelText: 'NIT',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _limiteCreditoController,
          decoration: const InputDecoration(
            labelText: 'Límite de crédito (opcional)',
            prefixText: 'Q ',
            border: OutlineInputBorder(),
          ),
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
        ),
        if (_errorCrear != null) ...[
          const SizedBox(height: 8),
          Text(
            _errorCrear!,
            style: TextStyle(color: AppColors.of(context).danger),
          ),
        ],
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _guardando
                    ? null
                    : () => setState(() => _modoCrear = false),
                child: const Text('Volver a buscar'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.of(context).primary,
                ),
                onPressed: _guardando ? null : _guardarClienteNuevo,
                child: _guardando
                    ? const SizedBox(
                        height: 16,
                        width: 16,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('Guardar y usar'),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
