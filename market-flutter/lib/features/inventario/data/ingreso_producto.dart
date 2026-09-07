import 'package:decimal/decimal.dart';

/// Un ingreso (compra) de un producto en una tienda del grupo — ver
/// `GET /inventario/tiendas/{id}/productos/{id}/ingresos/grupo` (market-backend).
class IngresoProducto {
  const IngresoProducto({
    required this.tiendaId,
    required this.tiendaNombre,
    required this.fecha,
    required this.cantidad,
    required this.costoUnitario,
    required this.proveedorNombre,
  });

  factory IngresoProducto.fromJson(Map<String, dynamic> json) {
    return IngresoProducto(
      tiendaId: json['tiendaId'] as int,
      tiendaNombre: json['tiendaNombre'] as String,
      fecha: DateTime.parse(json['fecha'] as String),
      cantidad: Decimal.parse(json['cantidad'] as String),
      costoUnitario: Decimal.parse(json['costoUnitario'] as String),
      proveedorNombre: json['proveedorNombre'] as String?,
    );
  }

  final int tiendaId;
  final String tiendaNombre;
  final DateTime fecha;
  final Decimal cantidad;
  final Decimal costoUnitario;

  /// `null` si la compra o el proveedor de origen ya no existen (ver mapper en el backend).
  final String? proveedorNombre;
}
