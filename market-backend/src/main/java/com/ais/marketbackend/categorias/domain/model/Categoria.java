package com.ais.marketbackend.categorias.domain.model;

import java.util.Objects;

public class Categoria {

    private final Long id;
    private String nombre;
    private String imagen;
    private EstadoCategoria estado;

    public Categoria(Long id, String nombre, String imagen, EstadoCategoria estado) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.imagen = imagen;
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static Categoria nueva(String nombre, String imagen) {
        return new Categoria(null, nombre, imagen, EstadoCategoria.ACTIVA);
    }

    public boolean estaActiva() {
        return estado == EstadoCategoria.ACTIVA;
    }

    public void actualizarDatos(String nombre, String imagen) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.imagen = imagen;
    }

    public void activar() {
        this.estado = EstadoCategoria.ACTIVA;
    }

    public void desactivar() {
        this.estado = EstadoCategoria.INACTIVA;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getImagen() {
        return imagen;
    }

    public EstadoCategoria getEstado() {
        return estado;
    }
}
