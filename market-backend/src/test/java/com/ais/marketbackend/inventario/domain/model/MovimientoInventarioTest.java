package com.ais.marketbackend.inventario.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MovimientoInventarioTest {

    @Test
    void cantidadCeroOMenorEsInvalida() {
        assertThatThrownBy(() -> MovimientoInventario.nuevo(
                1L, 2L, BigDecimal.ZERO, new BigDecimal("5.00"), TipoMovimiento.COMPRA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadFraccionariaEsInvalida() {
        assertThatThrownBy(() -> MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("2.5"), BigDecimal.ONE, TipoMovimiento.COMPRA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void costoUnitarioNegativoEsInvalido() {
        assertThatThrownBy(() -> MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("-1"), TipoMovimiento.COMPRA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tiposDeEntradaClasificanCorrectamente() {
        assertThat(TipoMovimiento.COMPRA.esEntrada()).isTrue();
        assertThat(TipoMovimiento.AJUSTE_POSITIVO.esEntrada()).isTrue();
        assertThat(TipoMovimiento.TRASLADO_ENTRADA.esEntrada()).isTrue();
        assertThat(TipoMovimiento.DEVOLUCION_CLIENTE.esEntrada()).isTrue();
        assertThat(TipoMovimiento.VENTA.esEntrada()).isFalse();
        assertThat(TipoMovimiento.AJUSTE_NEGATIVO.esEntrada()).isFalse();
        assertThat(TipoMovimiento.TRASLADO_SALIDA.esEntrada()).isFalse();
        assertThat(TipoMovimiento.DEVOLUCION_PROVEEDOR.esEntrada()).isFalse();
    }
}
