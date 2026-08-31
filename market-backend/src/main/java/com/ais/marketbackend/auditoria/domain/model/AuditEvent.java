package com.ais.marketbackend.auditoria.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Fila de auditoría, escrita directo (misma transacción que la operación auditada,
 * sin outbox — ver Fase 7, PLAN_MEJORAS.md, para por qué). Append-only en base de
 * datos (trigger, ver {@code auditoria/001-audit-event.xml}) — este modelo no expone
 * ningún método de mutación a propósito, solo lectura tras construirse.
 */
public class AuditEvent {

    private final Long id;
    private final Instant fecha;
    private final Long actorId;
    private final String actorUsername;
    private final Long tiendaId;
    private final String accion;
    private final String entidad;
    private final String entidadId;
    private final ResultadoAuditoria resultado;
    private final String correlationId;
    private final String detalle;

    public AuditEvent(
            Long id, Instant fecha, Long actorId, String actorUsername, Long tiendaId, String accion, String entidad,
            String entidadId, ResultadoAuditoria resultado, String correlationId, String detalle) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.tiendaId = tiendaId;
        this.accion = requerirNoVacio(accion, "accion");
        this.entidad = requerirNoVacio(entidad, "entidad");
        this.entidadId = entidadId;
        this.resultado = Objects.requireNonNull(resultado, "resultado");
        this.correlationId = correlationId;
        this.detalle = detalle;
    }

    public static AuditEvent nuevo(
            Long actorId, String actorUsername, Long tiendaId, String accion, String entidad, String entidadId,
            ResultadoAuditoria resultado, String correlationId, String detalle) {
        return new AuditEvent(
                null, Instant.now(), actorId, actorUsername, tiendaId, accion, entidad, entidadId, resultado,
                correlationId, detalle);
    }

    private static String requerirNoVacio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacío.");
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetalle() {
        return detalle;
    }
}
