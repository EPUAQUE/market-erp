package com.ais.marketbackend.auditoria.application.services.impl;

import com.ais.marketbackend.auditoria.application.dtos.AuditEventResumen;
import com.ais.marketbackend.auditoria.application.services.interfaces.AuditoriaService;
import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.auditoria.domain.repository.AuditEventRepository;
import com.ais.marketbackend.auditoria.domain.service.AuditoriaRegistrador;
import com.ais.marketbackend.shared.domain.Pagina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa tanto el puerto de aplicación ({@link AuditoriaService}, lectura
 * paginada — usado por {@code AuditoriaController}) como el puerto de dominio angosto
 * ({@link AuditoriaRegistrador}, escritura — usado por otros módulos vía inyección de
 * la interfaz de dominio, nunca de esta clase directo).
 */
@Service
public class AuditoriaServiceImpl implements AuditoriaService, AuditoriaRegistrador {

    private final AuditEventRepository auditEventRepository;

    public AuditoriaServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional
    public void registrar(AuditEvent evento) {
        auditEventRepository.save(evento);
    }

    @Override
    public Pagina<AuditEventResumen> listarTodo(int pagina, int tamano) {
        return auditEventRepository.listarTodo(pagina, tamano).map(this::toResumen);
    }

    @Override
    public Pagina<AuditEventResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return auditEventRepository.listarPorTienda(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private AuditEventResumen toResumen(AuditEvent evento) {
        return new AuditEventResumen(
                evento.getId(), evento.getFecha(), evento.getActorId(), evento.getActorUsername(),
                evento.getTiendaId(), evento.getAccion(), evento.getEntidad(), evento.getEntidadId(),
                evento.getResultado(), evento.getCorrelationId(), evento.getDetalle());
    }
}
