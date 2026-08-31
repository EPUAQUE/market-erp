package com.ais.marketbackend.auditoria.application.dtos;

import com.ais.marketbackend.auditoria.domain.model.ResultadoAuditoria;
import java.time.Instant;

public record AuditEventResumen(
        Long id, Instant fecha, Long actorId, String actorUsername, Long tiendaId, String accion, String entidad,
        String entidadId, ResultadoAuditoria resultado, String correlationId, String detalle) {
}
