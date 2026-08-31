package com.ais.marketbackend.notificaciones.domain.repository;

import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository {

    Notificacion save(Notificacion notificacion);

    Optional<Notificacion> findById(Long id);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<Notificacion> findByTiendaId(Long tiendaId);

    /** Fase 11 (PLAN_MEJORAS.md): se generan periódicamente y se acumulan por tienda sin límite natural. */
    Pagina<Notificacion> findByTiendaId(Long tiendaId, int pagina, int tamano);

    /** Sin paginar — uso interno (ej. resumen del dashboard). */
    List<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId);

    Pagina<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId, int pagina, int tamano);

    boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(Long tiendaId, TipoNotificacion tipo, Long referenciaId);
}
