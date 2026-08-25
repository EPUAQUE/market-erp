package com.ais.marketbackend.inventario.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InventarioTest {

    @Test
    void nuevoEmpiezaEnCero() {
        Inventario inventario = Inventario.nuevo(1L, 2L);

        assertThat(inventario.getExistenciaActual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(inventario.getCostoPromedioActual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void primeraCompraFijaCostoPromedioAlCostoDeEntrada() {
        Inventario inventario = Inventario.nuevo(1L, 2L);
        MovimientoInventario compra = MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA);

        inventario.aplicar(compra);

        assertThat(inventario.getExistenciaActual()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(inventario.getCostoPromedioActual()).isEqualByComparingTo(new BigDecimal("5.0000"));
    }

    @Test
    void segundaCompraRecalculaCostoPromedioPonderado() {
        Inventario inventario = Inventario.nuevo(1L, 2L);
        inventario.aplicar(MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA));
        inventario.aplicar(MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("7.00"), TipoMovimiento.COMPRA));

        // (10*5 + 10*7) / 20 = 6.00
        assertThat(inventario.getExistenciaActual()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(inventario.getCostoPromedioActual()).isEqualByComparingTo(new BigDecimal("6.0000"));
    }

    @Test
    void ventaDescuentaExistenciaSinAlterarCostoPromedio() {
        Inventario inventario = Inventario.nuevo(1L, 2L);
        inventario.aplicar(MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA));

        inventario.aplicar(MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("4"), new BigDecimal("5.00"), TipoMovimiento.VENTA));

        assertThat(inventario.getExistenciaActual()).isEqualByComparingTo(new BigDecimal("6"));
        assertThat(inventario.getCostoPromedioActual()).isEqualByComparingTo(new BigDecimal("5.0000"));
    }

    @Test
    void egresoMayorQueExistenciaLanzaStockInsuficiente() {
        Inventario inventario = Inventario.nuevo(1L, 2L);
        inventario.aplicar(MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("5"), new BigDecimal("5.00"), TipoMovimiento.COMPRA));

        MovimientoInventario venta = MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("6"), new BigDecimal("5.00"), TipoMovimiento.VENTA);

        assertThatThrownBy(() -> inventario.aplicar(venta)).isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void constructorRechazaExistenciaNegativa() {
        assertThatThrownBy(() -> new Inventario(1L, 1L, 2L, new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
