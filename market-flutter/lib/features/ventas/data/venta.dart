import 'package:decimal/decimal.dart';

class Venta {
  const Venta({
    required this.id,
    required this.clienteId,
    required this.estado,
    required this.total,
  });

  factory Venta.fromJson(Map<String, dynamic> json) {
    return Venta(
      id: json['id'] as int,
      clienteId: json['clienteId'] as int,
      estado: json['estado'] as String,
      total: Decimal.parse(json['total'] as String),
    );
  }

  final int id;
  final int clienteId;
  final String estado;
  final Decimal total;
}
