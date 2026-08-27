package com.ais.marketbackend.seguridad.domain.model;

import java.util.Set;

/**
 * Autorización efectiva de un usuario: unión de permisos de todos sus roles, tiendas
 * asignadas directamente y tiendas heredadas de sus grupos asignados (ver
 * {@code UsuarioGrupoTienda}). {@code grupoIds} vacío no implica alcance limitado a
 * cero tiendas — un usuario puede tener solo asignaciones de tienda individual.
 */
public record PermisosEfectivos(
        Long usuarioId, String username, Set<String> permisos, Set<Long> tiendaIds, boolean alcanceGlobal,
        Set<Long> grupoIds) {

    /** Compatibilidad con el código existente que no conoce grupos: sin asignaciones de grupo. */
    public PermisosEfectivos(
            Long usuarioId, String username, Set<String> permisos, Set<Long> tiendaIds, boolean alcanceGlobal) {
        this(usuarioId, username, permisos, tiendaIds, alcanceGlobal, Set.of());
    }

    public boolean tienePermiso(String codigo) {
        return permisos.contains(codigo);
    }

    public boolean puedeAccederATienda(Long tiendaId) {
        return alcanceGlobal || tiendaIds.contains(tiendaId);
    }

    public boolean puedeAccederAGrupo(Long grupoId) {
        return alcanceGlobal || grupoIds.contains(grupoId);
    }
}
