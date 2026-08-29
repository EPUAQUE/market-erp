package com.ais.marketbackend.seguridad.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    private static final String CHARSET_ESPERADO = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    @Test
    void generaVeinteCaracteresSinAmbiguos() {
        String password = TemporaryPasswordGenerator.generar();

        assertThat(password).hasSize(20);
        assertThat(password.chars().allMatch(c -> CHARSET_ESPERADO.indexOf(c) >= 0)).isTrue();
    }

    @Test
    void dosLlamadasProducenValoresDistintos() {
        String primera = TemporaryPasswordGenerator.generar();
        String segunda = TemporaryPasswordGenerator.generar();

        assertThat(primera).isNotEqualTo(segunda);
    }
}
