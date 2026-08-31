package com.ais.marketbackend.auditoria.api.dtos.responses;

import com.ais.marketbackend.auditoria.domain.model.ResultadoAuditoria;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditEventResponse {
    Long id;
    Instant fecha;
    Long actorId;
    String actorUsername;
    Long tiendaId;
    String accion;
    String entidad;
    String entidadId;
    ResultadoAuditoria resultado;
    String correlationId;
    String detalle;
}
