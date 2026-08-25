package com.ais.marketbackend.marcas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarcaTest {

    @Test
    void nuevaMarcaExponeSuNombre() {
        Marca marca = Marca.nueva("Nestlé");

        assertThat(marca.getNombre()).isEqualTo("Nestlé");
    }

    @Test
    void actualizarCambiaElNombre() {
        Marca marca = Marca.nueva("Nestlé");

        marca.actualizar("Nestlé S.A.");

        assertThat(marca.getNombre()).isEqualTo("Nestlé S.A.");
    }
}
