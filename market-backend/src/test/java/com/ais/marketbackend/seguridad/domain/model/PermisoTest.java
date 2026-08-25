package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PermisoTest {

    @Test
    void codigoValidoSeAceptaTalCual() {
        Permiso permiso = new Permiso(1L, "VENTAS_CREAR", "Crear ventas");

        assertThat(permiso.getCodigo()).isEqualTo("VENTAS_CREAR");
    }

    @Test
    void codigoSinGuionBajoSeRechaza() {
        assertThatThrownBy(() -> new Permiso(1L, "VENTAS", "sin acción"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void codigoEnMinusculasSeRechaza() {
        assertThatThrownBy(() -> new Permiso(1L, "ventas_crear", "minúsculas"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void codigoNuloSeRechaza() {
        assertThatThrownBy(() -> new Permiso(1L, null, "nulo"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
