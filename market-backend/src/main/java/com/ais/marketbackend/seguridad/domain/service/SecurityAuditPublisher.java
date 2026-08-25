package com.ais.marketbackend.seguridad.domain.service;

/**
 * Publica eventos de seguridad. La implementación de referencia escribe al logger
 * {@code SECURITY_AUDIT}; cuando exista el módulo de auditoría (outbox descrito en
 * {@code docs/auditoria.md}) esta interfaz se implementa contra {@code AuditPublisher}
 * sin tocar los productores.
 */
public interface SecurityAuditPublisher {

    void publicar(TipoEventoAuditoria tipo, String correlationId, String detalleSanitizado);
}
