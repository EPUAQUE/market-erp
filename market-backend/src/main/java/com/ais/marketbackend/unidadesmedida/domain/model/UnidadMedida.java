package com.ais.marketbackend.unidadesmedida.domain.model;

import java.util.Objects;

public class UnidadMedida {

    private final Long id;
    private String nombre;
    private String abreviacion;
    private EstadoUnidadMedida estado;

    public UnidadMedida(Long id, String nombre, String abreviacion, EstadoUnidadMedida estado) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.abreviacion = Objects.requireNonNull(abreviacion, "abreviacion");
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static UnidadMedida nueva(String nombre, String abreviacion) {
        return new UnidadMedida(null, nombre, abreviacion, EstadoUnidadMedida.ACTIVA);
    }

    public void actualizar(String nombre, String abreviacion) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.abreviacion = Objects.requireNonNull(abreviacion, "abreviacion");
    }

    public void activar() {
        this.estado = EstadoUnidadMedida.ACTIVA;
    }

    public void desactivar() {
        this.estado = EstadoUnidadMedida.INACTIVA;
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

    public EstadoUnidadMedida getEstado() {
        return estado;
    }
}
