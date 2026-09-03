package com.ais.marketbackend.marcas.domain.model;

import java.util.Objects;

public class Marca {

    private final Long id;
    private String nombre;
    private EstadoMarca estado;

    public Marca(Long id, String nombre, EstadoMarca estado) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static Marca nueva(String nombre) {
        return new Marca(null, nombre, EstadoMarca.ACTIVA);
    }

    public void actualizar(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public void activar() {
        this.estado = EstadoMarca.ACTIVA;
    }

    public void desactivar() {
        this.estado = EstadoMarca.INACTIVA;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoMarca getEstado() {
        return estado;
    }
}
