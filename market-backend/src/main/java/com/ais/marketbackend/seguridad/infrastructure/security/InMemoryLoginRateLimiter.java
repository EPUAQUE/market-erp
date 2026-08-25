package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;
import com.ais.marketbackend.seguridad.domain.service.LoginRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Token bucket en memoria, por IP y por hash de username. Implementación de
 * referencia para una sola instancia — ver seguridad-desarrolladores.md §8: en
 * multi-instancia se requiere un almacén compartido (ej. Redis).
 */
@Component
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final SeguridadProperties properties;

    public InMemoryLoginRateLimiter(SeguridadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void verificarPermitido(String claveIp, String usernameHash) {
        consumirOFallar("ip:" + claveIp);
        consumirOFallar("user:" + usernameHash);
    }

    private void consumirOFallar(String clave) {
        SeguridadProperties.Login config = properties.rateLimit().login();
        Bucket bucket = buckets.computeIfAbsent(clave, k -> new Bucket(config.capacity()));
        Duration esperaSiFalla = bucket.consumir(config, Instant.now());
        if (esperaSiFalla != null) {
            throw new RateLimitExcedidoException(esperaSiFalla);
        }
    }

    private static final class Bucket {

        private double tokens;
        private Instant ultimoRefill;

        Bucket(double capacidadInicial) {
            this.tokens = capacidadInicial;
            this.ultimoRefill = Instant.now();
        }

        synchronized Duration consumir(SeguridadProperties.Login config, Instant ahora) {
            double refillPorSegundo = config.refillTokens() / (double) Math.max(1, config.refillPeriod().toSeconds());
            double segundosTranscurridos = Duration.between(ultimoRefill, ahora).toMillis() / 1000.0;
            tokens = Math.min(config.capacity(), tokens + segundosTranscurridos * refillPorSegundo);
            ultimoRefill = ahora;

            if (tokens < 1.0) {
                double faltante = 1.0 - tokens;
                long segundosEspera = (long) Math.ceil(faltante / refillPorSegundo);
                return Duration.ofSeconds(Math.max(1, segundosEspera));
            }
            tokens -= 1.0;
            return null;
        }
    }
}
