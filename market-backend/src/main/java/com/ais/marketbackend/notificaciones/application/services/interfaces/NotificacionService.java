package com.ais.marketbackend.notificaciones.application.services.interfaces;

import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import java.util.List;

public interface NotificacionService {

    /** Escanea otros módulos en busca de condiciones nuevas y crea las notificaciones que falten. */
    List<NotificacionResumen> generar(Long tiendaId);

    NotificacionResumen marcarLeida(Long tiendaId, Long id);

    List<NotificacionResumen> listarPorTienda(Long tiendaId);

    List<NotificacionResumen> listarNoLeidasPorTienda(Long tiendaId);
}
