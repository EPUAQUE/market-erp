package com.ais.marketbackend.caja.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MovimientoCajaTest {

    @Test
    void montoCeroOMenorEsInvalido() {
        assertThatThrownBy(() -> MovimientoCaja.nuevo(TipoMovimientoCaja.INGRESO, "x", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conceptoEnBlancoEsInvalido() {
        assertThatThrownBy(() -> MovimientoCaja.nuevo(TipoMovimientoCaja.INGRESO, "   ", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
