package com.ais.marketbackend.seguridad.domain.model;

import java.time.Instant;

/**
 * Refresh token opaco. Solo se persiste su hash (ver infraestructura); este
 * agregado nunca conoce el valor en claro. Rotatorio y de un solo uso: cada
 * refresh exitoso revoca este token y crea uno nuevo con {@code tokenPadreId}
 * apuntando a este.
 */
public class RefreshToken {

    private final Long id;
    private final Long usuarioId;
    private final String tokenHash;
    private final Instant creadoEn;
    private final Instant expiraEn;
    private boolean revocado;
    private final Long tokenPadreId;

    public RefreshToken(
            Long id, Long usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn,
            boolean revocado, Long tokenPadreId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.revocado = revocado;
        this.tokenPadreId = tokenPadreId;
    }

    public static RefreshToken nuevo(Long usuarioId, String tokenHash, Instant creadoEn, Instant expiraEn, Long tokenPadreId) {
        return new RefreshToken(null, usuarioId, tokenHash, creadoEn, expiraEn, false, tokenPadreId);
    }

    public boolean estaVigente(Instant ahora) {
        return !revocado && ahora.isBefore(expiraEn);
    }

    public void revocar() {
        this.revocado = true;
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

    public boolean isRevocado() {
        return revocado;
    }

    public Long getTokenPadreId() {
        return tokenPadreId;
    }
}
