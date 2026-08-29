package com.ais.marketbackend.seguridad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Fase 4 (PLAN_MEJORAS.md): {@code buckets} crecía sin límite — un bucket por
 * cada IP/usuario distinto visto, nunca eliminado. Estas pruebas confirman que
 * {@code limpiarBucketsLlenos} elimina un bucket solo una vez que se recargó por
 * completo (nunca uno que todavía refleja intentos recientes) y que, tras
 * eliminarlo, el próximo intento simplemente ve una capacidad nueva — igual que
 * si el bucket nunca hubiera existido.
 */
class InMemoryLoginRateLimiterTest {

    private SeguridadProperties propiedadesConCapacidadUno(Duration refillPeriod) {
        return new SeguridadProperties(
                null, null, null, null,
                new SeguridadProperties.RateLimit(new SeguridadProperties.Login(1, 1, refillPeriod)),
                null, null);
    }

    @Test
    void noEliminaUnBucketQueTodaviaNoSeHaRecargado() {
        InMemoryLoginRateLimiter limiter =
                new InMemoryLoginRateLimiter(propiedadesConCapacidadUno(Duration.ofMinutes(1)));

        limiter.verificarPermitido("1.2.3.4", "hashUsuario");
        assertThatThrownBy(() -> limiter.verificarPermitido("1.2.3.4", "hashUsuario"))
                .isInstanceOf(RateLimitExcedidoException.class);

        int antes = limiter.cantidadDeBucketsParaPruebas();
        limiter.limpiarBucketsLlenos();

        assertThat(limiter.cantidadDeBucketsParaPruebas()).isEqualTo(antes);
        assertThatThrownBy(() -> limiter.verificarPermitido("1.2.3.4", "hashUsuario"))
                .isInstanceOf(RateLimitExcedidoException.class);
    }

    @Test
    void eliminaUnBucketUnaVezQueSeRecargoPorCompleto() throws InterruptedException {
        // refillPeriod().toSeconds() trunca a segundos enteros (Bucket.refillPorSegundo) —
        // un período menor a 1s se redondea a 1s para ese cálculo, así que el período más
        // corto observable es 1 segundo.
        InMemoryLoginRateLimiter limiter =
                new InMemoryLoginRateLimiter(propiedadesConCapacidadUno(Duration.ofSeconds(1)));

        limiter.verificarPermitido("1.2.3.4", "hashUsuario");
        assertThat(limiter.cantidadDeBucketsParaPruebas()).isEqualTo(2); // ip: y user:

        Thread.sleep(1100);
        limiter.limpiarBucketsLlenos();

        assertThat(limiter.cantidadDeBucketsParaPruebas()).isZero();
        limiter.verificarPermitido("1.2.3.4", "hashUsuario");
    }
}
