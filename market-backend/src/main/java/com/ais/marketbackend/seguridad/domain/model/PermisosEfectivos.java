package com.ais.marketbackend.seguridad.domain.model;

import java.util.Set;

/** Autorización efectiva de un usuario: unión de permisos de todos sus roles y tiendas asignadas. */
public record PermisosEfectivos(
        Long usuarioId, String username, Set<String> permisos, Set<Long> tiendaIds, boolean alcanceGlobal) {

    public boolean tienePermiso(String codigo) {
        return permisos.contains(codigo);
    }

    public boolean puedeAccederATienda(Long tiendaId) {
        return alcanceGlobal || tiendaIds.contains(tiendaId);
    }
}
