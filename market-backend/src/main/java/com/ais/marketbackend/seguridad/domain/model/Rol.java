package com.ais.marketbackend.seguridad.domain.model;

import java.util.Set;

/**
 * Catálogo de roles de negocio. {@code alcanceGlobal = true} (ej. ADMIN) permite
 * operar sobre cualquier tienda sin necesidad de asignación explícita en
 * {@link UsuarioTienda}.
 */
public class Rol {

    private final Long id;
    private final String nombre;
    private final boolean alcanceGlobal;
    private final Set<Permiso> permisos;

    public Rol(Long id, String nombre, boolean alcanceGlobal, Set<Permiso> permisos) {
        this.id = id;
        this.nombre = nombre;
        this.alcanceGlobal = alcanceGlobal;
        this.permisos = Set.copyOf(permisos);
    }

    public boolean tienePermiso(String codigoPermiso) {
        return permisos.stream().anyMatch(p -> p.getCodigo().equals(codigoPermiso));
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isAlcanceGlobal() {
        return alcanceGlobal;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
