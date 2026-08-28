import 'package:decimal/decimal.dart';
import '../domain/carrito.dart';

/// Espejo plano (sin Isar) de una venta encolada para sincronizar — el shape
/// que ve el resto de la app. La representación real en disco (Isar, solo
/// nativo) vive en `core/db/local_store_io.dart`; este archivo no importa
/// nada de Isar para poder compilarse también en web (ver
/// `core/db/local_store.dart`).
class VentaPendienteLocal {
  const VentaPendienteLocal({
    required this.id,
    required this.correlationId,
    required this.tiendaId,
    required this.clienteId,
    required this.clientePendienteLocalId,
    required this.lineas,
    required this.metodoPago,
    required this.montoACobrar,
    required this.creadaEn,
    required this.mensajeError,
  });

  final int id;
  final String correlationId;
  final int tiendaId;

  /// `null` cuando `clientePendienteLocalId` no lo es — ver
  /// `VentaPendienteIsar`.
  final int? clienteId;

  final int? clientePendienteLocalId;
  final List<LineaCarrito> lineas;
  final String metodoPago;

  /// `null` = venta 100% a crédito, sin cobro al sincronizar.
  final Decimal? montoACobrar;

  final DateTime creadaEn;

  /// `null` mientras no se haya intentado sincronizar o mientras siga en la
  /// cola por un fallo de red (reintentable). No-nulo = un fallo de negocio
  /// que un encargado debe revisar — el motor de sync no la vuelve a
  /// intentar sola.
  final String? mensajeError;
}

/// Datos de una venta nueva por encolar — sin `id` todavía (lo asigna el
/// `LocalStore` al guardarla).
class NuevaVentaPendiente {
  const NuevaVentaPendiente({
    required this.correlationId,
    required this.tiendaId,
    required this.clienteId,
    required this.clientePendienteLocalId,
    required this.lineas,
    required this.metodoPago,
    required this.montoACobrar,
    required this.creadaEn,
  });

  final String correlationId;
  final int tiendaId;

  /// Exactamente uno de `clienteId`/`clientePendienteLocalId` es no-nulo.
  final int? clienteId;
  final int? clientePendienteLocalId;
  final List<LineaCarrito> lineas;
  final String metodoPago;
  final Decimal? montoACobrar;
  final DateTime creadaEn;
}
