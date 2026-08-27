package com.ais.marketbackend.seguridad.domain.model;

/**
 * Asigna un usuario a un grupo de tiendas completo con un rol (p. ej. {@code ADMIN_GRUPO}):
 * el usuario opera sobre todas las tiendas de ese grupo, no solo una. Independiente de
 * {@link UsuarioTienda} — un usuario nunca tiene ambas para tiendas del mismo grupo (ver
 * {@code UsuarioServiceImpl}, que rechaza la combinación al asignar). {@code grupoTiendaId}
 * es un identificador plano: el módulo grupostienda es dueño de esa tabla.
 */
public class UsuarioGrupoTienda {

    private final Long id;
    private final Long usuarioId;
    private final Long grupoTiendaId;
    private final Rol rol;

    public UsuarioGrupoTienda(Long id, Long usuarioId, Long grupoTiendaId, Rol rol) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.grupoTiendaId = grupoTiendaId;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getGrupoTiendaId() {
        return grupoTiendaId;
    }

    public Rol getRol() {
        return rol;
    }
}
