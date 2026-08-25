package com.ais.marketbackend.seguridad.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.seguridad.domain.exception.PoliticaContrasenaException;
import org.junit.jupiter.api.Test;

class PoliticaContrasenaValidatorTest {

    @Test
    void contrasenaDentroDelRangoNoLanza() {
        assertThatCode(() -> PoliticaContrasenaValidator.validar("una-frase-larga-y-segura", 12, 64))
                .doesNotThrowAnyException();
    }

    @Test
    void contrasenaMasCortaQueElMinimoSeRechaza() {
        assertThatThrownBy(() -> PoliticaContrasenaValidator.validar("corta", 12, 64))
                .isInstanceOf(PoliticaContrasenaException.class);
    }

    @Test
    void contrasenaMasLargaQueElMaximoSeRechaza() {
        String demasiadoLarga = "a".repeat(65);

        assertThatThrownBy(() -> PoliticaContrasenaValidator.validar(demasiadoLarga, 12, 64))
                .isInstanceOf(PoliticaContrasenaException.class);
    }

    @Test
    void contrasenaVaciaSeRechaza() {
        assertThatThrownBy(() -> PoliticaContrasenaValidator.validar("", 12, 64))
                .isInstanceOf(PoliticaContrasenaException.class);
    }
}
