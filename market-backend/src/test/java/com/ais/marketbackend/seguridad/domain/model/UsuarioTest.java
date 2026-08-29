package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void nuevoUsuarioEstaActivoConVersionCero() {
        Usuario usuario = Usuario.nuevo("ana", "hash");

        assertThat(usuario.estaActivo()).isTrue();
        assertThat(usuario.getVersionSeguridad()).isZero();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void cambiarPasswordActualizaHashYSubeVersion() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");

        usuario.cambiarPassword("hash-nuevo");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(usuario.getVersionSeguridad()).isEqualTo(1L);
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
    }

    @Test
    void restablecerConPasswordTemporalMarcaLaCuentaYSubeVersion() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");

        usuario.restablecerConPasswordTemporal("hash-temporal");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-temporal");
        assertThat(usuario.isDebeCambiarPassword()).isTrue();
        assertThat(usuario.getVersionSeguridad()).isEqualTo(1L);
    }

    @Test
    void cambiarPasswordLimpiaLaMarcaDeDebeCambiar() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        usuario.restablecerConPasswordTemporal("hash-temporal");

        usuario.cambiarPassword("hash-definitivo");

        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-definitivo");
    }

    @Test
    void desactivarDejaDeEstarActivoYSubeVersion() {
        Usuario usuario = Usuario.nuevo("ana", "hash");

        usuario.desactivar();

        assertThat(usuario.estaActivo()).isFalse();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(usuario.getVersionSeguridad()).isEqualTo(1L);
    }

    @Test
    void bloquearDejaDeEstarActivo() {
        Usuario usuario = Usuario.nuevo("ana", "hash");

        usuario.bloquear();

        assertThat(usuario.estaActivo()).isFalse();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    @Test
    void activarRestauraElEstadoActivo() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        usuario.bloquear();

        usuario.activar();

        assertThat(usuario.estaActivo()).isTrue();
        assertThat(usuario.getVersionSeguridad()).isEqualTo(2L);
    }
}
