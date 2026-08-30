package com.ais.marketbackend.seguridad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityVersionValidatorTest {

    private UsuarioRepository usuarioRepository;
    private SecurityVersionValidator validator;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        validator = new SecurityVersionValidator(usuarioRepository);
    }

    @Test
    void tokenConVersionVigenteEsValido() {
        Usuario usuario = new Usuario(
                1L, "ana", "hash", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 3L,
                null, null, null, false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        assertThat(validator.validate(jwtCon("ana", 3L)).hasErrors()).isFalse();
    }

    @Test
    void tokenConVersionDesactualizadaEsInvalido() {
        Usuario usuario = new Usuario(
                1L, "ana", "hash", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 4L,
                null, null, null, false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        assertThat(validator.validate(jwtCon("ana", 3L)).hasErrors()).isTrue();
    }

    @Test
    void tokenDeUsuarioBloqueadoEsInvalidoAunqueLaVersionCoincida() {
        Usuario usuario = new Usuario(
                1L, "ana", "hash", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.BLOQUEADO, 3L,
                null, null, null, false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        assertThat(validator.validate(jwtCon("ana", 3L)).hasErrors()).isTrue();
    }

    @Test
    void tokenDeUsuarioInexistenteEsInvalido() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThat(validator.validate(jwtCon("fantasma", 0L)).hasErrors()).isTrue();
    }

    @Test
    void tokenSinClaimSverEsInvalido() {
        Jwt jwt = new Jwt(
                "jwt-value", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "RS256"), Map.of("sub", "ana"));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private Jwt jwtCon(String username, long sver) {
        return new Jwt(
                "jwt-value", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "RS256"), Map.of("sub", username, "sver", sver));
    }
}
