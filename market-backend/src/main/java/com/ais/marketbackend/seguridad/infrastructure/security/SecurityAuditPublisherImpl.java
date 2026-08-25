package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.domain.service.TipoEventoAuditoria;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación de referencia: escribe al logger {@code SECURITY_AUDIT}, igual
 * que el {@code SecurityAuditService} descrito en {@code docs/auditoria.md}. Cuando
 * exista el módulo de auditoría (outbox), esta clase se reemplaza por un adaptador
 * a {@code AuditPublisher} sin tocar los productores (ver {@link SecurityAuditPublisher}).
 * Nunca registra contraseñas, hashes ni tokens completos — solo el detalle ya
 * sanitizado que recibe.
 *
 * <p>También incrementa {@code market.security.evento} por {@link TipoEventoAuditoria} —
 * único punto reusado por todos los productores (login, refresh, logout, etc.), así
 * que cubre las métricas de "refresh reutilizados" pedidas en la Fase 7 sin agregar
 * contadores dispersos por cada llamador.
 */
@Component
public class SecurityAuditPublisherImpl implements SecurityAuditPublisher {

    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final MeterRegistry meterRegistry;

    public SecurityAuditPublisherImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publicar(TipoEventoAuditoria tipo, String correlationId, String detalleSanitizado) {
        SECURITY_AUDIT.info("evento={} correlationId={} detalle={}", tipo, correlationId, detalleSanitizado);
        meterRegistry.counter("market.security.evento", "tipo", tipo.name()).increment();
    }
}
