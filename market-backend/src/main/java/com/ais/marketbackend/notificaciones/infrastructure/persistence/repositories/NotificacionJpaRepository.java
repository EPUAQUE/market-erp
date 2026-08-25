package com.ais.marketbackend.notificaciones.infrastructure.persistence.repositories;

import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.entities.NotificacionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByTiendaId(Long tiendaId);

    List<NotificacionEntity> findByTiendaIdAndLeidaFalse(Long tiendaId);

    boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(Long tiendaId, TipoNotificacion tipo, Long referenciaId);
}
