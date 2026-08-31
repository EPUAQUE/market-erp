package com.ais.marketbackend.notificaciones.application.services.interfaces;

import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;

public interface NotificacionService {

    /** Escanea otros módulos en busca de condiciones nuevas y crea las notificaciones que falten. */
    List<NotificacionResumen> generar(Long tiendaId);

    NotificacionResumen marcarLeida(Long tiendaId, Long id);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<NotificacionResumen> listarPorTienda(Long tiendaId);

    Pagina<NotificacionResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);

    /** Sin paginar — uso interno (ej. resumen del dashboard). El endpoint público usa la variante paginada. */
    List<NotificacionResumen> listarNoLeidasPorTienda(Long tiendaId);

    Pagina<NotificacionResumen> listarNoLeidasPorTienda(Long tiendaId, int pagina, int tamano);
}
