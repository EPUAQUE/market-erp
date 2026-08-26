package com.ais.marketbackend.seguridad.application.services.impl;

import com.ais.marketbackend.seguridad.application.dtos.LoginResult;
import com.ais.marketbackend.seguridad.application.services.interfaces.AuthService;
import com.ais.marketbackend.seguridad.domain.exception.AutenticacionFallidaException;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.RefreshToken;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.AccessTokenIssuer;
import com.ais.marketbackend.seguridad.domain.service.LoginRateLimiter;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.seguridad.domain.service.RefreshTokenCrypto;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.domain.service.TipoEventoAuditoria;
import com.ais.marketbackend.seguridad.domain.service.UsernameCanonicalizer;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final PermisosEfectivosResolver permisosEfectivosResolver;
    private final LoginRateLimiter loginRateLimiter;
    private final SecurityAuditPublisher auditPublisher;
    private final Duration refreshTokenTtl;

    /** Hash Argon2id de una contraseña ficticia, para ejecutar matches() aun sin usuario real. */
    private final String credencialFicticia;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenIssuer accessTokenIssuer,
            PermisosEfectivosResolver permisosEfectivosResolver,
            LoginRateLimiter loginRateLimiter,
            SecurityAuditPublisher auditPublisher,
            SeguridadProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.permisosEfectivosResolver = permisosEfectivosResolver;
        this.loginRateLimiter = loginRateLimiter;
        this.auditPublisher = auditPublisher;
        this.refreshTokenTtl = properties.refreshToken().ttl();
        this.credencialFicticia = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public LoginResult login(String username, String passwordPlano, String claveIp) {
        String correlationId = UUID.randomUUID().toString();
        String usernameCanonico = UsernameCanonicalizer.canonicalizar(username);
        String usernameHash = RefreshTokenCrypto.hash(usernameCanonico);

        loginRateLimiter.verificarPermitido(claveIp, usernameHash);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(usernameCanonico);
        String hashParaVerificar = usuarioOpt.map(Usuario::getPasswordHash).orElse(credencialFicticia);
        boolean coincide = passwordEncoder.matches(passwordPlano, hashParaVerificar);

        if (usuarioOpt.isEmpty() || !coincide || !usuarioOpt.get().estaActivo()) {
            auditPublisher.publicar(TipoEventoAuditoria.LOGIN_FALLIDO, correlationId, "login=" + usernameHash);
            throw new AutenticacionFallidaException();
        }

        Usuario usuario = usuarioOpt.get();
        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(usuario.getId());
        exigirAlcanceOAuditarYRechazar(usuario.getId(), permisos, correlationId, TipoEventoAuditoria.LOGIN_FALLIDO);
        LoginResult resultado = emitirTokens(usuario, permisos);
        auditPublisher.publicar(TipoEventoAuditoria.LOGIN_EXITOSO, correlationId, "usuarioId=" + usuario.getId());
        return resultado;
    }

    /**
     * Un rol de alcance global (ADMIN) no necesita tienda asignada; cualquier otro rol
     * sí — sin al menos una fila en {@code usuario_tienda}, el usuario no tiene sobre qué
     * operar. Mismo mensaje/código genérico que cualquier otra falla de autenticación
     * (ver el comentario de clase de {@link AutenticacionFallidaException}): esto nunca
     * debe distinguirse en la respuesta de un login con credenciales incorrectas.
     */
    private void exigirAlcanceOAuditarYRechazar(
            Long usuarioId, PermisosEfectivos permisos, String correlationId, TipoEventoAuditoria tipoEventoFallo) {
        if (!permisos.alcanceGlobal() && permisos.tiendaIds().isEmpty()) {
            auditPublisher.publicar(
                    tipoEventoFallo, correlationId, "usuarioId=" + usuarioId + ",motivo=sin_tienda_asignada");
            throw new AutenticacionFallidaException();
        }
    }

    /**
     * Consume el token con un {@code UPDATE} condicional atómico
     * ({@link RefreshTokenRepository#consumir}) en vez de la secuencia no protegida
     * find→comprobar→save: ante dos refresh simultáneos con el mismo token, la base
     * de datos garantiza que como máximo una llamada lo marca revocado, sin importar
     * el orden de llegada. La rama perdedora (0 filas actualizadas) siempre se trata
     * como reutilización — incluida una carrera legítima entre dos requests del mismo
     * cliente — y revoca toda la familia: es la postura conservadora que pide la Fase 5
     * del plan ("el segundo intento activa la política de reutilización").
     *
     * {@code noRollbackFor}: sin esto, la revocación de familia hecha en
     * {@code manejarConsumoFallido} antes de lanzar {@code AutenticacionFallidaException}
     * se deshace — el rollback por defecto de Spring ante cualquier RuntimeException
     * revierte también el {@code UPDATE} de revocación, dejando la respuesta en 401
     * pero la familia intacta en la base de datos. Detectado verificando la Fase 5 con
     * concurrencia real: la reutilización genuina (reproducir un token ya consumido)
     * respondía 401 pero no revocaba nada.
     */
    @Override
    @Transactional(noRollbackFor = AutenticacionFallidaException.class)
    public LoginResult refresh(String refreshTokenPlano) {
        String correlationId = UUID.randomUUID().toString();
        String hash = RefreshTokenCrypto.hash(refreshTokenPlano);
        Instant ahora = Instant.now();

        boolean consumido = refreshTokenRepository.consumir(hash, ahora) == 1;
        if (!consumido) {
            manejarConsumoFallido(hash, correlationId);
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(AutenticacionFallidaException::new);
        Usuario usuario = usuarioRepository.findById(token.getUsuarioId())
                .filter(Usuario::estaActivo)
                .orElseThrow(AutenticacionFallidaException::new);

        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(usuario.getId());
        exigirAlcanceOAuditarYRechazar(usuario.getId(), permisos, correlationId, TipoEventoAuditoria.REFRESH_FALLIDO);
        LoginResult resultado = emitirTokensRotados(usuario, permisos, token.getId());
        auditPublisher.publicar(TipoEventoAuditoria.REFRESH_EXITOSO, correlationId, "usuarioId=" + usuario.getId());
        return resultado;
    }

    private void manejarConsumoFallido(String hash, String correlationId) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(hash);
        if (tokenOpt.isEmpty()) {
            auditPublisher.publicar(TipoEventoAuditoria.REFRESH_FALLIDO, correlationId, "token no encontrado");
            throw new AutenticacionFallidaException();
        }

        RefreshToken token = tokenOpt.get();
        if (token.isRevocado()) {
            refreshTokenRepository.revocarTodosDeUsuario(token.getUsuarioId());
            auditPublisher.publicar(
                    TipoEventoAuditoria.REFRESH_REUTILIZADO, correlationId, "usuarioId=" + token.getUsuarioId());
            throw new AutenticacionFallidaException();
        }

        auditPublisher.publicar(TipoEventoAuditoria.REFRESH_FALLIDO, correlationId, "token expirado");
        throw new AutenticacionFallidaException();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenPlano) {
        String hash = RefreshTokenCrypto.hash(refreshTokenPlano);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.revocar();
            refreshTokenRepository.save(token);
            auditPublisher.publicar(TipoEventoAuditoria.LOGOUT, UUID.randomUUID().toString(),
                    "usuarioId=" + token.getUsuarioId());
        });
    }

    private LoginResult emitirTokens(Usuario usuario, PermisosEfectivos permisos) {
        return emitirTokensRotados(usuario, permisos, null);
    }

    private LoginResult emitirTokensRotados(Usuario usuario, PermisosEfectivos permisos, Long tokenPadreId) {
        AccessTokenIssuer.Resultado accessToken = accessTokenIssuer.emitir(usuario, permisos);

        String refreshPlano = RefreshTokenCrypto.generarOpaco();
        Instant ahora = Instant.now();
        RefreshToken nuevoRefresh = RefreshToken.nuevo(
                usuario.getId(), RefreshTokenCrypto.hash(refreshPlano), ahora, ahora.plus(refreshTokenTtl), tokenPadreId);
        refreshTokenRepository.save(nuevoRefresh);

        long expiresInSeconds = Duration.between(ahora, accessToken.expiraEn()).toSeconds();
        return new LoginResult(accessToken.token(), refreshPlano, expiresInSeconds);
    }
}
