package com.ais.marketbackend.unidadesmedida.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnidadMedidaTest {

    @Test
    void nuevaUnidadMedidaExponeSusDatos() {
        UnidadMedida unidad = UnidadMedida.nueva("Kilogramo", "kg");

        assertThat(unidad.getNombre()).isEqualTo("Kilogramo");
        assertThat(unidad.getAbreviacion()).isEqualTo("kg");
    }

    @Test
    void actualizarCambiaNombreYAbreviacion() {
        UnidadMedida unidad = UnidadMedida.nueva("Kilogramo", "kg");

        unidad.actualizar("Kilogramos", "Kg.");

        assertThat(unidad.getNombre()).isEqualTo("Kilogramos");
        assertThat(unidad.getAbreviacion()).isEqualTo("Kg.");
    }
}
