package com.ais.marketbackend.notificaciones.domain.repository;

import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository {

    Notificacion save(Notificacion notificacion);

    Optional<Notificacion> findById(Long id);

    List<Notificacion> findByTiendaId(Long tiendaId);

    List<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId);

    boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(Long tiendaId, TipoNotificacion tipo, Long referenciaId);
}
