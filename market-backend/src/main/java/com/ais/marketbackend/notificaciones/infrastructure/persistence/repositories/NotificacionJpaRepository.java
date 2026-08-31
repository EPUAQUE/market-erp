package com.ais.marketbackend.notificaciones.infrastructure.persistence.repositories;

import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.entities.NotificacionEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByTiendaId(Long tiendaId);

    Page<NotificacionEntity> findByTiendaId(Long tiendaId, Pageable pageable);

    List<NotificacionEntity> findByTiendaIdAndLeidaFalse(Long tiendaId);

    Page<NotificacionEntity> findByTiendaIdAndLeidaFalse(Long tiendaId, Pageable pageable);

    boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(Long tiendaId, TipoNotificacion tipo, Long referenciaId);
}
