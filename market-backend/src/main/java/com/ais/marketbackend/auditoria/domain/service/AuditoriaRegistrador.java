package com.ais.marketbackend.auditoria.domain.service;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;

/**
 * Puerto de dominio angosto para que OTROS módulos (seguridad, el aspecto
 * {@code @Auditable}) puedan registrar un evento de auditoría sin depender de la
 * capa de aplicación completa de {@code auditoria} (que también expone lectura
 * paginada, fuera de lo que un productor necesita). Implementado por
 * {@code AuditoriaServiceImpl}.
 */
public interface AuditoriaRegistrador {

    void registrar(AuditEvent evento);
}
