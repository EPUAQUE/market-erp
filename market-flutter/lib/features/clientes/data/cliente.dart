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

/// Lo que devuelve `ClienteSelectorSheet` — un [Cliente] real (con id de
/// servidor) o la referencia a un cliente recién creado offline que todavía
/// no tiene uno. Antes, un cliente creado sin conexión no podía usarse en
/// ninguna venta de la misma sesión offline (la hoja se cerraba sin
/// devolver nada); ahora `CheckoutNotifier`/`SyncEngineNotifier` saben
/// resolver [pendienteLocalId] al id real una vez que ese cliente
/// sincroniza (ver `VentaPendienteLocal.clientePendienteLocalId`).
class ClienteSeleccionado {
  ClienteSeleccionado.sincronizado(Cliente cliente)
    : id = cliente.id,
      pendienteLocalId = null,
      nombre = cliente.nombre,
      nit = cliente.nit,
      limiteCredito = cliente.limiteCredito;

  const ClienteSeleccionado.pendienteLocal({
    required this.pendienteLocalId,
    required this.nombre,
    required this.nit,
    required this.limiteCredito,
  }) : id = null;

  /// Id real de servidor — `null` si todavía no sincroniza.
  final int? id;

  /// Id local (Isar) del `ClientePendienteIsar` — `null` si ya es un
  /// cliente real (`id` no nulo). Exactamente uno de los dos es no-nulo.
  final int? pendienteLocalId;

  final String nombre;
  final String? nit;
  final Decimal? limiteCredito;

  bool get esPendienteLocal => pendienteLocalId != null;
}
