package com.ais.marketbackend.seguridad.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsernameCanonicalizerTest {

    @Test
    void recortaEspaciosYPasaAMinusculas() {
        assertThat(UsernameCanonicalizer.canonicalizar("  Ana.Lopez  ")).isEqualTo("ana.lopez");
    }

    @Test
    void mismoUsernameConDistintoCasoCanonicalizaIgual() {
        assertThat(UsernameCanonicalizer.canonicalizar("ADMIN"))
                .isEqualTo(UsernameCanonicalizer.canonicalizar("admin"));
    }

    @Test
    void nuloSeMantieneNulo() {
        assertThat(UsernameCanonicalizer.canonicalizar(null)).isNull();
    }
}
