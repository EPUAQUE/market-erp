package com.ais.marketbackend.productos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductoTiendaTest {

    @Test
    void nuevaConfiguracionEstaActivaPorDefecto() {
        ProductoTienda pt = ProductoTienda.nueva(
                1L, 1L, new BigDecimal("10.50"), new BigDecimal("5"), new BigDecimal("100"), true, true);

        assertThat(pt.isActivo()).isTrue();
        assertThat(pt.permiteVenta()).isTrue();
        assertThat(pt.permiteIngresoDeInventario()).isTrue();
    }

    @Test
    void precioNegativoSeRechaza() {
        assertThatThrownBy(() -> ProductoTienda.nueva(
                1L, 1L, new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.TEN, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stockMinimoMayorQueMaximoSeRechaza() {
        assertThatThrownBy(() -> ProductoTienda.nueva(
                1L, 1L, BigDecimal.TEN, new BigDecimal("50"), new BigDecimal("10"), true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void permitirIngresoFalseBloqueaIngresoPeroNoVenta() {
        ProductoTienda pt = ProductoTienda.nueva(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, false);

        assertThat(pt.permiteIngresoDeInventario()).isFalse();
        assertThat(pt.permiteVenta()).isTrue();
    }

    @Test
    void desactivarBloqueaVentaEIngresoAunqueLasBanderasSiganEnTrue() {
        ProductoTienda pt = ProductoTienda.nueva(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);

        pt.desactivar();

        assertThat(pt.permiteVenta()).isFalse();
        assertThat(pt.permiteIngresoDeInventario()).isFalse();
    }

    @Test
    void actualizarConfiguracionCambiaPrecioYBanderas() {
        ProductoTienda pt = ProductoTienda.nueva(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);

        pt.actualizarConfiguracion(new BigDecimal("20"), BigDecimal.ONE, new BigDecimal("30"), false, true);

        assertThat(pt.getPrecioVenta()).isEqualTo(new BigDecimal("20"));
        assertThat(pt.isPermitirVenta()).isFalse();
        assertThat(pt.isPermitirIngreso()).isTrue();
    }
}
