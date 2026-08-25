package com.ais.marketbackend.marcas.domain.model;

import java.util.Objects;

public class Marca {

    private final Long id;
    private String nombre;

    public Marca(Long id, String nombre) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public static Marca nueva(String nombre) {
        return new Marca(null, nombre);
    }

    public void actualizar(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
