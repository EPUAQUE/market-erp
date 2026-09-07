import 'package:decimal/decimal.dart';

/// Existencia de un producto en una tienda del grupo — ver
/// `GET /inventario/tiendas/{id}/productos/{id}/grupo` (market-backend).
class ExistenciaTienda {
  const ExistenciaTienda({
    required this.tiendaId,
    required this.tiendaNombre,
    required this.existenciaActual,
  });

  factory ExistenciaTienda.fromJson(Map<String, dynamic> json) {
    return ExistenciaTienda(
      tiendaId: json['tiendaId'] as int,
      tiendaNombre: json['tiendaNombre'] as String,
      existenciaActual: Decimal.parse(json['existenciaActual'] as String),
    );
  }

  final int tiendaId;
  final String tiendaNombre;
  final Decimal existenciaActual;
}
