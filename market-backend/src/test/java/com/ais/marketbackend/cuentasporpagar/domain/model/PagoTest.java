package com.ais.marketbackend.cuentasporpagar.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PagoTest {

    @Test
    void montoCeroOMenorEsInvalido() {
        assertThatThrownBy(() -> Pago.nuevo(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
