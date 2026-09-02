package com.ais.marketbackend.traslados.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LineaTrasladoTest {

    @Test
    void cantidadCeroOMenorEsInvalida() {
        assertThatThrownBy(() -> LineaTraslado.nueva(1L, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadFraccionariaEsInvalida() {
        assertThatThrownBy(() -> LineaTraslado.nueva(1L, new BigDecimal("2.5")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
