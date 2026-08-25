package com.ais.marketbackend.compras.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.compras.domain.exception.CompraSinLineasException;
import com.ais.marketbackend.compras.domain.exception.EstadoCompraInvalidoException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompraTest {

    @Test
    void nuevaCompraEmpiezaEnBorrador() {
        Compra compra = Compra.nueva(1L, 1L, List.of(LineaCompra.nueva(1L, new BigDecimal("10"), new BigDecimal("5.00"))));

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.BORRADOR);
    }

    @Test
    void nuevaCompraSinLineasLanzaExcepcion() {
        assertThatThrownBy(() -> Compra.nueva(1L, 1L, List.of()))
                .isInstanceOf(CompraSinLineasException.class);
    }

    @Test
    void totalSumaElSubtotalDeCadaLinea() {
        Compra compra = Compra.nueva(1L, 1L, List.of(
                LineaCompra.nueva(1L, new BigDecimal("10"), new BigDecimal("5.00")),
                LineaCompra.nueva(2L, new BigDecimal("2"), new BigDecimal("3.00"))));

        assertThat(compra.total()).isEqualByComparingTo(new BigDecimal("56.00"));
    }

    @Test
    void recibirTransicionaABorradorARecibida() {
        Compra compra = Compra.nueva(1L, 1L, List.of(LineaCompra.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)));

        compra.recibir();

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.RECIBIDA);
    }

    @Test
    void recibirUnaCompraYaRecibidaLanzaExcepcion() {
        Compra compra = Compra.nueva(1L, 1L, List.of(LineaCompra.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)));
        compra.recibir();

        assertThatThrownBy(compra::recibir).isInstanceOf(EstadoCompraInvalidoException.class);
    }

    @Test
    void anularUnaCompraRecibidaLanzaExcepcion() {
        Compra compra = Compra.nueva(1L, 1L, List.of(LineaCompra.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)));
        compra.recibir();

        assertThatThrownBy(compra::anular).isInstanceOf(EstadoCompraInvalidoException.class);
    }

    @Test
    void anularUnaCompraEnBorradorLaDejaAnulada() {
        Compra compra = Compra.nueva(1L, 1L, List.of(LineaCompra.nueva(1L, BigDecimal.ONE, BigDecimal.ONE)));

        compra.anular();

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.ANULADA);
    }
}
