enum EstadoCategoria { activa, inactiva }

EstadoCategoria _estadoCategoriaFromJson(String value) =>
    value == 'ACTIVA' ? EstadoCategoria.activa : EstadoCategoria.inactiva;

class Categoria {
  const Categoria({
    required this.id,
    required this.nombre,
    required this.estado,
  });

  factory Categoria.fromJson(Map<String, dynamic> json) {
    return Categoria(
      id: json['id'] as int,
      nombre: json['nombre'] as String,
      estado: _estadoCategoriaFromJson(json['estado'] as String),
    );
  }

  final int id;
  final String nombre;
  final EstadoCategoria estado;

  bool get activa => estado == EstadoCategoria.activa;
}
