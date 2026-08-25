/// Autorización efectiva del usuario autenticado — mismo shape que el
/// `/api/v1/auth/me` del backend (`MeResponse`), sin re-derivar nada aquí.
class SesionUsuario {
  const SesionUsuario({
    required this.username,
    required this.permisos,
    required this.tiendaIds,
    required this.alcanceGlobal,
  });

  factory SesionUsuario.fromJson(Map<String, dynamic> json) {
    return SesionUsuario(
      username: json['username'] as String,
      permisos: Set<String>.from(
        json['permisos'] as List<dynamic>? ?? const [],
      ),
      tiendaIds: Set<int>.from(
        (json['tiendaIds'] as List<dynamic>? ?? const []).map((e) => e as int),
      ),
      alcanceGlobal: json['alcanceGlobal'] as bool? ?? false,
    );
  }

  final String username;
  final Set<String> permisos;
  final Set<int> tiendaIds;
  final bool alcanceGlobal;

  bool can(String permissionCode) => permisos.contains(permissionCode);

  bool canAccessTienda(int tiendaId) =>
      alcanceGlobal || tiendaIds.contains(tiendaId);
}
