package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void tokenNuevoEstaVigenteAntesDeExpirar() {
        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.nuevo(1L, "hash", ahora, ahora.plus(1, ChronoUnit.DAYS), null);

        assertThat(token.estaVigente(ahora)).isTrue();
    }

    @Test
    void tokenExpiradoNoEstaVigente() {
        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.nuevo(1L, "hash", ahora.minus(2, ChronoUnit.DAYS), ahora.minus(1, ChronoUnit.DAYS), null);

        assertThat(token.estaVigente(ahora)).isFalse();
    }

    @Test
    void tokenRevocadoNoEstaVigenteAunSinExpirar() {
        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.nuevo(1L, "hash", ahora, ahora.plus(1, ChronoUnit.DAYS), null);

        token.revocar();

        assertThat(token.estaVigente(ahora)).isFalse();
        assertThat(token.isRevocado()).isTrue();
    }
}
