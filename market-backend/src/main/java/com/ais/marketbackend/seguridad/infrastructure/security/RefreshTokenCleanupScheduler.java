package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purga periódica de refresh tokens ya expirados — evita que la tabla crezca sin
 * límite (un token nuevo por cada login/refresh). No toca tokens revocados-pero-aún-
 * no-expirados: se conservan hasta su expiración natural por si hiciera falta
 * auditar una reutilización detectada.
 */
@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedDelayString = "${app.security.refresh-token.cleanup-interval:PT1H}")
    @Transactional
    public void limpiarExpirados() {
        int eliminados = refreshTokenRepository.eliminarExpirados(Instant.now());
        if (eliminados > 0) {
            log.info("Limpieza de refresh tokens expirados: {} eliminados.", eliminados);
        }
    }
}
