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
import com.ais.marketbackend.seguridad.domain.exception.TokenResetInvalidoException;
import com.ais.marketbackend.seguridad.domain.model.PasswordResetToken;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.RefreshToken;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.PasswordResetTokenRepository;
import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.AccessTokenIssuer;
import com.ais.marketbackend.seguridad.domain.service.LoginRateLimiter;
import com.ais.marketbackend.seguridad.domain.service.PasswordResetMailSender;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordResetMailSender passwordResetMailSender;
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
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordResetMailSender = mock(PasswordResetMailSender.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accessTokenIssuer = mock(AccessTokenIssuer.class);
        permisosEfectivosResolver = mock(PermisosEfectivosResolver.class);
        loginRateLimiter = mock(LoginRateLimiter.class);
        auditPublisher = mock(SecurityAuditPublisher.class);

        when(passwordEncoder.encode(anyString())).thenReturn(CREDENCIAL_FICTICIA);

        SeguridadProperties properties = new SeguridadProperties(
                null,
                new SeguridadProperties.RefreshToken(Duration.ofDays(30)),
                new SeguridadProperties.PasswordReset(Duration.ofMinutes(30)),
                null,
                new SeguridadProperties.PasswordPolicy(12, 64),
                null, null, null);

        authService = new AuthServiceImpl(
                usuarioRepository, refreshTokenRepository, passwordResetTokenRepository, passwordResetMailSender,
                passwordEncoder, accessTokenIssuer, permisosEfectivosResolver, loginRateLimiter, auditPublisher,
                properties);

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
        assertThat(resultado.debeCambiarPassword()).isFalse();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void loginConUsuarioQueDebeCambiarPasswordLoIndicaEnElResultado() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        usuario.restablecerConPasswordTemporal("hash-temporal");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("temporal123", "hash-temporal")).thenReturn(true);
        when(permisosEfectivosResolver.resolver(any()))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of("VENTAS_VER"), Set.of(1L), false));

        LoginResult resultado = authService.login("ana", "temporal123", "127.0.0.1");

        assertThat(resultado.debeCambiarPassword()).isTrue();
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

    @Test
    void solicitarRestablecimientoConUsuarioElegibleEnviaCorreoYGuardaToken() {
        Usuario usuario = new Usuario(
                1L, "ana", "hash-real", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 0L,
                "Ana", "5555-5555", "ana@correo.com", false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.solicitarRestablecimiento("ana", "127.0.0.1");

        verify(loginRateLimiter).verificarPermitido(eq("127.0.0.1"), anyString());
        verify(passwordResetTokenRepository).invalidarNoUsadosDeUsuario(1L);
        verify(passwordResetTokenRepository).save(any());
        verify(passwordResetMailSender).enviar(eq("ana@correo.com"), anyString());
    }

    @Test
    void solicitarRestablecimientoConUsuarioInexistenteNoEnviaNadaNiFalla() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        authService.solicitarRestablecimiento("no-existe", "127.0.0.1");

        verify(passwordResetMailSender, never()).enviar(anyString(), anyString());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void solicitarRestablecimientoConUsuarioSinCorreoNoEnviaNadaNiFalla() {
        Usuario usuario = Usuario.nuevo("ana", "hash-real");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        authService.solicitarRestablecimiento("ana", "127.0.0.1");

        verify(passwordResetMailSender, never()).enviar(anyString(), anyString());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void solicitarRestablecimientoConUsuarioBloqueadoNoEnviaNadaNiFalla() {
        Usuario usuario = new Usuario(
                1L, "ana", "hash-real", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.BLOQUEADO, 0L,
                "Ana", "5555-5555", "ana@correo.com", false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        authService.solicitarRestablecimiento("ana", "127.0.0.1");

        verify(passwordResetMailSender, never()).enviar(anyString(), anyString());
    }

    @Test
    void solicitarRestablecimientoRespetaElRateLimit() {
        org.mockito.Mockito.doThrow(
                        new com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException(Duration.ofSeconds(30)))
                .when(loginRateLimiter).verificarPermitido(anyString(), anyString());

        assertThatThrownBy(() -> authService.solicitarRestablecimiento("ana", "127.0.0.1"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException.class);

        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    @Test
    void restablecerPasswordConTokenValidoCambiaLaPasswordYRevocaSesiones() {
        PasswordResetToken token = PasswordResetToken.nuevo(1L, "hash-token", Instant.now(), Instant.now().plusSeconds(1800));
        Usuario usuario = new Usuario(
                1L, "ana", "hash-viejo", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 0L,
                null, null, null, false);
        when(passwordResetTokenRepository.consumir(anyString(), any())).thenReturn(1);
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nuevaClaveSegura1")).thenReturn("hash-nuevo");

        authService.restablecerPassword("token-plano", "nuevaClaveSegura1");

        verify(usuarioRepository).save(usuario);
        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void restablecerPasswordConTokenYaUsadoOExpiradoLanzaExcepcionGenerica() {
        when(passwordResetTokenRepository.consumir(anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> authService.restablecerPassword("token-plano", "nuevaClaveSegura1"))
                .isInstanceOf(TokenResetInvalidoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void restablecerPasswordConPasswordQueNoCumpleLaPoliticaNoConsumeElToken() {
        assertThatThrownBy(() -> authService.restablecerPassword("token-plano", "corta"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.PoliticaContrasenaException.class);

        verify(passwordResetTokenRepository, never()).consumir(anyString(), any());
    }
}
