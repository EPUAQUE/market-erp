package com.ais.marketbackend.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.ventas.domain.exception.EstadoVentaInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.VentaSinLineasException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class VentaTest {

    @Test
    void nuevaVentaEmpiezaEnBorrador() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, new BigDecimal("10"), new BigDecimal("5.00"))),
                MetodoPago.EFECTIVO);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.BORRADOR);
    }

    @Test
    void nuevaVentaExponeElMetodoPago() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.TARJETA);

        assertThat(venta.getMetodoPago()).isEqualTo(MetodoPago.TARJETA);
    }

    @Test
    void nuevaVentaSinCorrelationIdLoExponeComoNull() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO);

        assertThat(venta.getCorrelationId()).isNull();
    }

    @Test
    void nuevaVentaConCorrelationIdLoExpone() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO, "abc-123");

        assertThat(venta.getCorrelationId()).isEqualTo("abc-123");
    }

    @Test
    void nuevaVentaSinMetodoPagoLanzaExcepcion() {
        assertThatThrownBy(() -> Venta.nueva(1L, 1L, 1L,
                List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nuevaVentaSinLineasLanzaExcepcion() {
        assertThatThrownBy(() -> Venta.nueva(1L, 1L, 1L, List.of(), MetodoPago.EFECTIVO))
                .isInstanceOf(VentaSinLineasException.class);
    }

    @Test
    void totalSumaElSubtotalDeCadaLinea() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(
                LineaVenta.nueva(1L, new BigDecimal("10"), new BigDecimal("5.00")),
                LineaVenta.nueva(2L, new BigDecimal("2"), new BigDecimal("3.00"))),
                MetodoPago.EFECTIVO);

        assertThat(venta.total()).isEqualByComparingTo(new BigDecimal("56.00"));
    }

    @Test
    void completarTransicionaDeBorradorACompletada() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO);

        venta.completar();

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.COMPLETADA);
    }

    @Test
    void completarUnaVentaYaCompletadaLanzaExcepcion() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO);
        venta.completar();

        assertThatThrownBy(venta::completar).isInstanceOf(EstadoVentaInvalidoException.class);
    }

    @Test
    void anularUnaVentaCompletadaLanzaExcepcion() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO);
        venta.completar();

        assertThatThrownBy(venta::anular).isInstanceOf(EstadoVentaInvalidoException.class);
    }

    @Test
    void anularUnaVentaEnBorradorLaDejaAnulada() {
        Venta venta = Venta.nueva(1L, 1L, 1L, List.of(LineaVenta.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO);

        venta.anular();

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
    }
}
