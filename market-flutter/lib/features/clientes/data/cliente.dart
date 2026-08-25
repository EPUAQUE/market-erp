import 'package:decimal/decimal.dart';

class Cliente {
  const Cliente({
    required this.id,
    required this.nit,
    required this.nombre,
    required this.telefono,
    required this.estado,
    required this.limiteCredito,
  });

  factory Cliente.fromJson(Map<String, dynamic> json) {
    return Cliente(
      id: json['id'] as int,
      nit: json['nit'] as String?,
      nombre: json['nombre'] as String,
      telefono: json['telefono'] as String?,
      estado: json['estado'] as String,
      limiteCredito: json['limiteCredito'] != null
          ? Decimal.parse(json['limiteCredito'] as String)
          : null,
    );
  }

  final int id;
  final String? nit;
  final String nombre;
  final String? telefono;
  final String estado;

  /// `null` = sin límite definido/evaluado todavía (ver CLAUDE.md) — no es
  /// lo mismo que Q0.
  final Decimal? limiteCredito;

  bool coincideBusqueda(String query) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return true;
    return nombre.toLowerCase().contains(q) ||
        (nit?.toLowerCase().contains(q) ?? false) ||
        (telefono?.contains(q) ?? false);
  }
}
