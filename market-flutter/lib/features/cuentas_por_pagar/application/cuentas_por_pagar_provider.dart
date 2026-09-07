import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/cuenta_por_pagar_api.dart';

final cuentaPorPagarApiProvider = Provider<CuentaPorPagarApi>(
  (ref) => CuentaPorPagarApi(ApiClient.instance),
);

/// Cuentas por pagar `PENDIENTE` de una tienda, más vencida primero — mismo
/// criterio que `cuentasPorCobrarPendientesProvider`.
final cuentasPorPagarPendientesProvider = FutureProvider.autoDispose
    .family<List<CuentaPorPagar>, int>((ref, tiendaId) async {
      final cuentas = await ref
          .watch(cuentaPorPagarApiProvider)
          .listarPorTienda(tiendaId);
      final pendientes = cuentas.where((c) => c.pendiente).toList()
        ..sort((a, b) => a.fechaVencimiento.compareTo(b.fechaVencimiento));
      return pendientes;
    });
