package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;
import com.ais.marketbackend.seguridad.domain.service.LoginRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Token bucket en memoria, por IP y por hash de username. Implementación de
 * referencia para una sola instancia — ver seguridad-desarrolladores.md §8: en
 * multi-instancia se requiere un almacén compartido (ej. Redis).
 *
 * <p>{@code buckets} crece con cada IP/usuario distinto visto — sin
 * {@code limpiarBucketsLlenos}, un atacante rotando IPs (o el tráfico normal a lo
 * largo del tiempo) lo haría crecer indefinidamente. Un bucket ya recargado hasta
 * su capacidad es indistinguible de uno recién creado (mismo estado inicial), así
 * que eliminarlo es seguro — la próxima solicitud simplemente crea uno nuevo
 * idéntico vía {@code computeIfAbsent}.
 */
@Component
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLoginRateLimiter.class);

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

    /**
     * Sin esto, el mapa crece sin límite (Fase 4, PLAN_MEJORAS.md). Se apoya en la
     * semántica de {@link ConcurrentHashMap#entrySet()}: si otra solicitud consume
     * un token del mismo bucket justo entre la lectura y el borrado, esa
     * disminución se pierde y el próximo intento parte de capacidad completa —
     * equivalente a que ese intento hubiera llegado un instante después con el
     * bucket ya recién creado. No debilita el límite más allá de ese margen.
     */
    @Scheduled(fixedDelayString = "${app.security.rate-limit.login.cleanup-interval:PT10M}")
    void limpiarBucketsLlenos() {
        SeguridadProperties.Login config = properties.rateLimit().login();
        Instant ahora = Instant.now();
        int antes = buckets.size();
        buckets.entrySet().removeIf(entry -> entry.getValue().estaLlenoTrasRecargar(config, ahora));
        int eliminados = antes - buckets.size();
        if (eliminados > 0) {
            log.debug("Limpieza de rate limiter de login: {} buckets llenos eliminados.", eliminados);
        }
    }

    /** Solo para pruebas — expone cuántos buckets hay en memoria en este momento. */
    int cantidadDeBucketsParaPruebas() {
        return buckets.size();
    }

    private static final class Bucket {

        private double tokens;
        private Instant ultimoRefill;

        Bucket(double capacidadInicial) {
            this.tokens = capacidadInicial;
            this.ultimoRefill = Instant.now();
        }

        synchronized Duration consumir(SeguridadProperties.Login config, Instant ahora) {
            recargar(config, ahora);
            if (tokens < 1.0) {
                double refillPorSegundo = refillPorSegundo(config);
                double faltante = 1.0 - tokens;
                long segundosEspera = (long) Math.ceil(faltante / refillPorSegundo);
                return Duration.ofSeconds(Math.max(1, segundosEspera));
            }
            tokens -= 1.0;
            return null;
        }

        synchronized boolean estaLlenoTrasRecargar(SeguridadProperties.Login config, Instant ahora) {
            recargar(config, ahora);
            return tokens >= config.capacity();
        }

        private void recargar(SeguridadProperties.Login config, Instant ahora) {
            double segundosTranscurridos = Duration.between(ultimoRefill, ahora).toMillis() / 1000.0;
            tokens = Math.min(config.capacity(), tokens + segundosTranscurridos * refillPorSegundo(config));
            ultimoRefill = ahora;
        }

        private double refillPorSegundo(SeguridadProperties.Login config) {
            return config.refillTokens() / (double) Math.max(1, config.refillPeriod().toSeconds());
        }
    }
}
