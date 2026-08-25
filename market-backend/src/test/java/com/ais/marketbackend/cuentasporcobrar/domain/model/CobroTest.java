package com.ais.marketbackend.cuentasporcobrar.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CobroTest {

    @Test
    void montoCeroOMenorEsInvalido() {
        assertThatThrownBy(() -> Cobro.nuevo(BigDecimal.ZERO, MetodoPago.EFECTIVO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
