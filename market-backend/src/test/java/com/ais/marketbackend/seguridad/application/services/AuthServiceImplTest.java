package com.ais.marketbackend.seguridad.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.seguridad.application.dtos.LoginResult;
import com.ais.marketbackend.seguridad.application.services.impl.AuthServiceImpl;
import com.ais.marketbackend.seguridad.domain.exception.AutenticacionFallidaException;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.RefreshToken;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.AccessTokenIssuer;
import com.ais.marketbackend.seguridad.domain.service.LoginRateLimiter;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AccessTokenIssuer accessTokenIssuer;
    private PermisosEfectivosResolver permisosEfectivosResolver;
    private LoginRateLimiter loginRateLimiter;
    private SecurityAuditPublisher auditPublisher;
    private AuthServiceImpl authService;

    private static final String CREDENCIAL_FICTICIA = "argon2-dummy-hash";

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accessTokenIssuer = mock(AccessTokenIssuer.class);
        permisosEfectivosResolver = mock(PermisosEfectivosResolver.class);
        loginRateLimiter = mock(LoginRateLimiter.class);
        auditPublisher = mock(SecurityAuditPublisher.class);

        when(passwordEncoder.encode(anyString())).thenReturn(CREDENCIAL_FICTICIA);

        SeguridadProperties properties = new SeguridadProperties(
                null,
                new SeguridadProperties.RefreshToken(Duration.ofDays(30)),
                null, null, null, null, null);

        authService = new AuthServiceImpl(
                usuarioRepository, refreshTokenRepository, passwordEncoder, accessTokenIssuer,
                permisosEfectivosResolver, loginRateLimiter, auditPublisher, properties);

        when(accessTokenIssuer.emitir(any(), any()))
                .thenReturn(new AccessTokenIssuer.Resultado("jwt-token", Instant.now().plusSeconds(600)));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loginExitosoEmiteTokensYAuditaExito() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123456", "hash-real")).thenReturn(true);
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of("VENTAS_VER"), Set.of(1L), false));

        LoginResult resultado = authService.login("ana", "clave123456", "127.0.0.1");

        assertThat(resultado.accessToken()).isEqualTo("jwt-token");
        assertThat(resultado.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void loginConUsuarioInexistenteIgualQuePasswordIncorrecta() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), eq(CREDENCIAL_FICTICIA))).thenReturn(false);

        assertThatThrownBy(() -> authService.login("no-existe", "cualquiera", "127.0.0.1"))
                .isInstanceOf(AutenticacionFallidaException.class);

        verify(passwordEncoder).matches("cualquiera", CREDENCIAL_FICTICIA);
    }

    @Test
    void loginConPasswordIncorrectaLanzaExcepcionGenerica() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala-clave", "hash-real")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ana", "mala-clave", "127.0.0.1"))
                .isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void loginSinTiendaAsignadaYSinAlcanceGlobalSeRechaza() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123456", "hash-real")).thenReturn(true);
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of(), Set.of(), false));

        assertThatThrownBy(() -> authService.login("ana", "clave123456", "127.0.0.1"))
                .isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginConAlcanceGlobalNoNecesitaTiendaAsignada() {
        Usuario usuario = Usuario.nuevo("admin", "hash-real");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123456", "hash-real")).thenReturn(true);
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "admin", Set.of("USUARIOS_CREAR"), Set.of(), true));

        LoginResult resultado = authService.login("admin", "clave123456", "127.0.0.1");

        assertThat(resultado.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginConUsuarioBloqueadoSeRechazaAunConPasswordCorrecta() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        usuario.bloquear();
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123456", "hash-real")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("ana", "clave123456", "127.0.0.1"))
                .isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void loginRespetaElRateLimitAntesDeConsultarUsuario() {
        org.mockito.Mockito.doThrow(
                        new com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException(Duration.ofSeconds(30)))
                .when(loginRateLimiter).verificarPermitido(anyString(), anyString());

        assertThatThrownBy(() -> authService.login("ana", "clave123456", "127.0.0.1"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException.class);

        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    @Test
    void refreshValidoConsumeElTokenAtomicamenteYEmiteUnoNuevo() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        RefreshToken tokenExistente = RefreshToken.nuevo(1L, "hash-presentado", Instant.now(), Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(1);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExistente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of(), Set.of(1L), false));

        LoginResult resultado = authService.refresh("token-plano");

        assertThat(resultado.accessToken()).isEqualTo("jwt-token");
        verify(refreshTokenRepository).consumir(anyString(), any());
        verify(refreshTokenRepository, times(1)).save(any());
        verify(refreshTokenRepository, never()).revocarTodosDeUsuario(any());
    }

    @Test
    void refreshConTokenNoEncontradoLanzaExcepcionGenerica() {
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(0);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("token-desconocido"))
                .isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void refreshConTokenYaRevocadoRevocaTodaLaCadenaYAuditaReuso() {
        RefreshToken tokenYaUsado = RefreshToken.nuevo(1L, "hash", Instant.now(), Instant.now().plusSeconds(3600), null);
        tokenYaUsado.revocar();
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(0);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenYaUsado));

        assertThatThrownBy(() -> authService.refresh("token-reutilizado"))
                .isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void refreshConTokenExpiradoNoRevocaLaCadenaCompleta() {
        Instant pasado = Instant.now().minusSeconds(10);
        RefreshToken tokenExpirado = RefreshToken.nuevo(1L, "hash", pasado.minusSeconds(100), pasado, null);
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(0);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> authService.refresh("token-expirado"))
                .isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository, never()).revocarTodosDeUsuario(any());
    }

    @Test
    void refreshSinTiendaAsignadaYSinAlcanceGlobalSeRechaza() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        RefreshToken tokenExistente = RefreshToken.nuevo(1L, "hash", Instant.now(), Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(1);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExistente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of(), Set.of(), false));

        assertThatThrownBy(() -> authService.refresh("token-plano"))
                .isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshConUsuarioYaNoActivoSeRechaza() {
        Usuario usuarioBloqueado = Usuario.nuevo("ana", "hash-real");
        usuarioBloqueado.bloquear();
        RefreshToken tokenExistente = RefreshToken.nuevo(1L, "hash", Instant.now(), Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(1);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExistente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBloqueado));

        assertThatThrownBy(() -> authService.refresh("token-plano"))
                .isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void refreshConDosIntentosSimultaneosSoloUnoConsumeElToken() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        RefreshToken tokenExistente = RefreshToken.nuevo(1L, "hash", Instant.now(), Instant.now().plusSeconds(3600), null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of(), Set.of(1L), false));
        when(refreshTokenRepository.consumir(anyString(), any())).thenReturn(1, 0);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExistente));

        LoginResult resultadoGanador = authService.refresh("token-plano");
        assertThat(resultadoGanador.accessToken()).isEqualTo("jwt-token");

        tokenExistente.revocar();
        assertThatThrownBy(() -> authService.refresh("token-plano"))
                .isInstanceOf(AutenticacionFallidaException.class);
        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void logoutRevocaElTokenEncontrado() {
        RefreshToken token = RefreshToken.nuevo(1L, "hash", Instant.now(), Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout("token-plano");

        assertThat(token.isRevocado()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logoutConTokenInexistenteNoLanzaNiGuarda() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout("token-desconocido");

        verify(refreshTokenRepository, never()).save(any());
    }
}
