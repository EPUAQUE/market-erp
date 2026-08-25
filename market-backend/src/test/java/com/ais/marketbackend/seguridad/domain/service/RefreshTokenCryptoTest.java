package com.ais.marketbackend.seguridad.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenCryptoTest {

    @Test
    void generarOpacoProduceValoresDistintosCadaVez() {
        String primero = RefreshTokenCrypto.generarOpaco();
        String segundo = RefreshTokenCrypto.generarOpaco();

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(primero).isNotBlank();
    }

    @Test
    void hashEsDeterministaParaElMismoValor() {
        String token = RefreshTokenCrypto.generarOpaco();

        assertThat(RefreshTokenCrypto.hash(token)).isEqualTo(RefreshTokenCrypto.hash(token));
    }

    @Test
    void hashDeValoresDistintosProduceHashesDistintos() {
        assertThat(RefreshTokenCrypto.hash("a")).isNotEqualTo(RefreshTokenCrypto.hash("b"));
    }

    @Test
    void hashNuncaContieneElValorOriginalEnClaro() {
        String token = "token-super-secreto";

        assertThat(RefreshTokenCrypto.hash(token)).doesNotContain(token);
    }
}
