package com.ais.marketbackend.seguridad.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Código de permiso plano, convención {@code MODULO_ACCION} en MAYÚSCULAS_SNAKE. */
public class Permiso {

    private static final Pattern CODIGO_VALIDO = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$");

    private final Long id;
    private final String codigo;
    private final String descripcion;

    public Permiso(Long id, String codigo, String descripcion) {
        if (codigo == null || !CODIGO_VALIDO.matcher(codigo).matches()) {
            throw new IllegalArgumentException("Código de permiso inválido: " + codigo);
        }
        this.id = id;
        this.codigo = codigo;
        this.descripcion = Objects.requireNonNullElse(descripcion, "");
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permiso permiso)) return false;
        return codigo.equals(permiso.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
