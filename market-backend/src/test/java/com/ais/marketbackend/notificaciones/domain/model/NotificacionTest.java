package com.ais.marketbackend.notificaciones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NotificacionTest {

    @Test
    void nuevaEmpiezaNoLeida() {
        Notificacion notificacion = Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, "Stock bajo");

        assertThat(notificacion.isLeida()).isFalse();
        assertThat(notificacion.getFechaLectura()).isNull();
    }

    @Test
    void marcarLeidaRegistraLaFechaDeLectura() {
        Notificacion notificacion = Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, "Stock bajo");

        notificacion.marcarLeida();

        assertThat(notificacion.isLeida()).isTrue();
        assertThat(notificacion.getFechaLectura()).isNotNull();
    }

    @Test
    void marcarLeidaDosVecesEsIdempotente() {
        Notificacion notificacion = Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, "Stock bajo");
        notificacion.marcarLeida();
        var primeraFechaLectura = notificacion.getFechaLectura();

        notificacion.marcarLeida();

        assertThat(notificacion.getFechaLectura()).isEqualTo(primeraFechaLectura);
    }

    @Test
    void mensajeVacioEsInvalido() {
        assertThatThrownBy(() -> Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
