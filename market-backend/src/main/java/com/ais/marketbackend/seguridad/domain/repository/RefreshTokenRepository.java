package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findChain(Long usuarioId);

    void revocarTodosDeUsuario(Long usuarioId);

    /**
     * Consume el token atómicamente: {@code UPDATE ... SET revocado = true WHERE
     * token_hash = :tokenHash AND revocado = false AND expira_en > :ahora}. Devuelve
     * 1 si esta llamada fue la que lo consumió, 0 si ya estaba revocado o expirado —
     * la única fuente de verdad ante refresh simultáneos con el mismo token, sin la
     * secuencia no protegida find→comprobar→save.
     */
    int consumir(String tokenHash, Instant ahora);

    /** Elimina tokens expirados antes de {@code antesDe}; devuelve cuántos se borraron. */
    int eliminarExpirados(Instant antesDe);
}
