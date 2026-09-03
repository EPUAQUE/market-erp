package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    @Test
    void tokenNuevoEstaVigenteAntesDeExpirar() {
        Instant ahora = Instant.now();
        PasswordResetToken token = PasswordResetToken.nuevo(1L, "hash", ahora, ahora.plus(30, ChronoUnit.MINUTES));

        assertThat(token.estaVigente(ahora)).isTrue();
    }

    @Test
    void tokenExpiradoNoEstaVigente() {
        Instant ahora = Instant.now();
        PasswordResetToken token = PasswordResetToken.nuevo(
                1L, "hash", ahora.minus(1, ChronoUnit.HOURS), ahora.minus(30, ChronoUnit.MINUTES));

        assertThat(token.estaVigente(ahora)).isFalse();
    }

    @Test
    void tokenUsadoNoEstaVigenteAunSinExpirar() {
        Instant ahora = Instant.now();
        PasswordResetToken token = PasswordResetToken.nuevo(1L, "hash", ahora, ahora.plus(30, ChronoUnit.MINUTES));

        token.marcarUsado();

        assertThat(token.estaVigente(ahora)).isFalse();
        assertThat(token.isUsado()).isTrue();
    }
}
