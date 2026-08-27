package com.ais.marketbackend.grupostienda.domain.model;

import java.util.Objects;

/**
 * Agregado raíz de un grupo de tiendas (un negocio/cliente distinto sobre la misma
 * instalación). {@code codigo} es el identificador de negocio público — inmutable una
 * vez creado el grupo, igual que {@code Tienda.codigo}.
 */
public class GrupoTienda {

    private final Long id;
    private final String codigo;
    private String nombre;
    private EstadoGrupoTienda estado;

    public GrupoTienda(Long id, String codigo, String nombre, EstadoGrupoTienda estado) {
        this.id = id;
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static GrupoTienda nuevo(String codigo, String nombre) {
        return new GrupoTienda(null, codigo, nombre, EstadoGrupoTienda.ACTIVO);
    }

    public boolean estaActivo() {
        return estado == EstadoGrupoTienda.ACTIVO;
    }

    public void actualizarDatos(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public void activar() {
        this.estado = EstadoGrupoTienda.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoGrupoTienda.INACTIVO;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoGrupoTienda getEstado() {
        return estado;
    }
}
