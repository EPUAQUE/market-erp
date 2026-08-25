package com.ais.marketbackend.seguridad.domain.model;

/**
 * Asigna un usuario a una tienda con un rol. Determina el alcance de datos sobre
 * el que el usuario puede operar (no solo qué puede hacer, ver {@link Rol}).
 * {@code tiendaId} es un identificador plano: el módulo Tiendas es dueño de esa
 * tabla y de su validación de existencia; Seguridad solo la referencia.
 */
public class UsuarioTienda {

    private final Long id;
    private final Long usuarioId;
    private final Long tiendaId;
    private final Rol rol;

    public UsuarioTienda(Long id, Long usuarioId, Long tiendaId, Rol rol) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tiendaId = tiendaId;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Rol getRol() {
        return rol;
    }
}
