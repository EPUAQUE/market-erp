class Proveedor {
  const Proveedor({required this.id, required this.nombre, required this.nit});

  factory Proveedor.fromJson(Map<String, dynamic> json) {
    return Proveedor(
      id: json['id'] as int,
      nombre: json['nombre'] as String,
      nit: json['nit'] as String?,
    );
  }

  final int id;
  final String nombre;
  final String? nit;

  bool coincideBusqueda(String query) {
    final q = query.trim().toLowerCase();
    if (q.isEmpty) return true;
    return nombre.toLowerCase().contains(q) ||
        (nit?.toLowerCase().contains(q) ?? false);
  }
}
