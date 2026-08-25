package com.ais.marketbackend.notificaciones.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Alerta generada a partir de una condición detectada en otro módulo (cuenta
 * vencida, gasto programado vencido, stock bajo) — ver {@code
 * NotificacionServiceImpl.generar}, que hace polling de esos módulos vía sus
 * puertos {@code application.services.interfaces}. No se crea manualmente por
 * el usuario vía API.
 */
public class Notificacion {

    private final Long id;
    private final Long tiendaId;
    private final TipoNotificacion tipo;
    private final Long referenciaId;
    private final String mensaje;
    private final Instant fecha;
    private boolean leida;
    private Instant fechaLectura;

    public Notificacion(
            Long id, Long tiendaId, TipoNotificacion tipo, Long referenciaId, String mensaje, Instant fecha,
            boolean leida, Instant fechaLectura) {
        this.id = id;
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.referenciaId = Objects.requireNonNull(referenciaId, "referenciaId");
        this.mensaje = requerirMensaje(mensaje);
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.leida = leida;
        this.fechaLectura = fechaLectura;
    }

    public static Notificacion nueva(Long tiendaId, TipoNotificacion tipo, Long referenciaId, String mensaje) {
        return new Notificacion(null, tiendaId, tipo, referenciaId, mensaje, Instant.now(), false, null);
    }

    /** Idempotente: marcar dos veces la misma notificación como leída no es un error. */
    public void marcarLeida() {
        if (leida) {
            return;
        }
        this.leida = true;
        this.fechaLectura = Instant.now();
    }

    private static String requerirMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío.");
        }
        return mensaje;
    }

    public Long getId() {
        return id;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Instant getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }

    public Instant getFechaLectura() {
        return fechaLectura;
    }
}
