package com.ais.marketbackend.notificaciones.api.dtos.responses;

import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificacionResponse {

    Long id;
    Long tiendaId;
    TipoNotificacion tipo;
    Long referenciaId;
    String mensaje;
    Instant fecha;
    boolean leida;
    Instant fechaLectura;
}
