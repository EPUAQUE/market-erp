package com.ais.marketbackend.traslados.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.traslados.domain.exception.EstadoTrasladoInvalidoException;
import com.ais.marketbackend.traslados.domain.exception.TrasladoMismaTiendaException;
import com.ais.marketbackend.traslados.domain.exception.TrasladoSinLineasException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrasladoTest {

    @Test
    void nuevoTrasladoEmpiezaEnBorrador() {
        Traslado traslado = Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN)));

        assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.BORRADOR);
    }

    @Test
    void nuevoTrasladoSinLineasLanzaExcepcion() {
        assertThatThrownBy(() -> Traslado.nuevo(1L, 2L, List.of())).isInstanceOf(TrasladoSinLineasException.class);
    }

    @Test
    void nuevoTrasladoConMismaTiendaOrigenYDestinoLanzaExcepcion() {
        assertThatThrownBy(() -> Traslado.nuevo(1L, 1L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN))))
                .isInstanceOf(TrasladoMismaTiendaException.class);
    }

    @Test
    void completarTransicionaDeBorradorACompletado() {
        Traslado traslado = Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN)));

        traslado.completar();

        assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.COMPLETADO);
    }

    @Test
    void completarUnTrasladoYaCompletadoLanzaExcepcion() {
        Traslado traslado = Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN)));
        traslado.completar();

        assertThatThrownBy(traslado::completar).isInstanceOf(EstadoTrasladoInvalidoException.class);
    }

    @Test
    void anularUnTrasladoCompletadoLanzaExcepcion() {
        Traslado traslado = Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN)));
        traslado.completar();

        assertThatThrownBy(traslado::anular).isInstanceOf(EstadoTrasladoInvalidoException.class);
    }

    @Test
    void anularUnTrasladoEnBorradorLoDejaAnulado() {
        Traslado traslado = Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(1L, BigDecimal.TEN)));

        traslado.anular();

        assertThat(traslado.getEstado()).isEqualTo(EstadoTraslado.ANULADO);
    }
}
