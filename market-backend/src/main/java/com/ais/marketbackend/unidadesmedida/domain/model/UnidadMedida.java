package com.ais.marketbackend.unidadesmedida.domain.model;

import java.util.Objects;

/** Catálogo de unidades (ej. "Kilogramo" / "kg"). Sin estado: es un catálogo puro. */
public class UnidadMedida {

    private final Long id;
    private String nombre;
    private String abreviacion;

    public UnidadMedida(Long id, String nombre, String abreviacion) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.abreviacion = Objects.requireNonNull(abreviacion, "abreviacion");
    }

    public static UnidadMedida nueva(String nombre, String abreviacion) {
        return new UnidadMedida(null, nombre, abreviacion);
    }

    public void actualizar(String nombre, String abreviacion) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.abreviacion = Objects.requireNonNull(abreviacion, "abreviacion");
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAbreviacion() {
        return abreviacion;
    }
}
