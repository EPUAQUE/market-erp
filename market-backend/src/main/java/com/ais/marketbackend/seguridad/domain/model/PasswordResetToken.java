package com.ais.marketbackend.seguridad.domain.model;

import java.time.Instant;

/**
 * Token opaco de un solo uso para el flujo público "olvidé mi contraseña". Solo se
 * persiste su hash (ver infraestructura); este agregado nunca conoce el valor en
 * claro — mismo criterio que {@link RefreshToken}. A diferencia de
 * {@code RefreshToken}, no es rotatorio: se marca {@code usado} y no se reemplaza.
 */
public class PasswordResetToken {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final Instant creadoEn;
    private final Instant expiraEn;
    private boolean usado;

    public PasswordResetToken(
            Long id, Long usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn, boolean usado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.usado = usado;
    }

    public static PasswordResetToken nuevo(Long usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn) {
        return new PasswordResetToken(null, usuarioId, tokenHash, creadoEn, expiraEn, false);
    }

    public boolean estaVigente(Instant ahora) {
        return !usado && ahora.isBefore(expiraEn);
    }

    public void marcarUsado() {
        this.usado = true;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public boolean isUsado() {
        return usado;
    }
}
