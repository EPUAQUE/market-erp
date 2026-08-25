import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../ventas/application/checkout_notifier.dart';
import '../../ventas/data/venta_api.dart';

/// Cuentas por cobrar `PENDIENTE` de una tienda, más vencida primero. Reusa
/// `cuentaPorCobrarApiProvider` (definido junto a `CheckoutNotifier`, que ya
/// lo necesitaba para el cobro automático post-venta) — no hay razón para un
/// segundo `Provider<CuentaPorCobrarApi>`.
final cuentasPorCobrarPendientesProvider = FutureProvider.autoDispose
    .family<List<CuentaPorCobrar>, int>((ref, tiendaId) async {
      final cuentas = await ref
          .watch(cuentaPorCobrarApiProvider)
          .listarPorTienda(tiendaId);
      final pendientes = cuentas.where((c) => c.pendiente).toList()
        ..sort((a, b) => a.fechaVencimiento.compareTo(b.fechaVencimiento));
      return pendientes;
    });
