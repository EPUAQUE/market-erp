package com.ais.marketbackend.notificaciones.application.dtos;

import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import java.time.Instant;

public record NotificacionResumen(
        Long id, Long tiendaId, TipoNotificacion tipo, Long referenciaId, String mensaje, Instant fecha,
        boolean leida, Instant fechaLectura) {
}
