package com.ais.marketbackend.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LineaVentaTest {

    @Test
    void subtotalMultiplicaCantidadPorPrecioUnitario() {
        LineaVenta linea = LineaVenta.nueva(1L, new BigDecimal("4"), new BigDecimal("2.50"));

        assertThat(linea.subtotal()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void cantidadCeroOMenorEsInvalida() {
        assertThatThrownBy(() -> LineaVenta.nueva(1L, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void precioUnitarioNegativoEsInvalido() {
        assertThatThrownBy(() -> LineaVenta.nueva(1L, BigDecimal.ONE, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
