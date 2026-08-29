package com.ais.marketbackend.caja.infrastructure.persistence.repositories;

import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.caja.infrastructure.persistence.entities.CajaSesionEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CajaSesionJpaRepository extends JpaRepository<CajaSesionEntity, Long> {

    Optional<CajaSesionEntity> findByTiendaIdAndEstado(Long tiendaId, EstadoCajaSesion estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CajaSesionEntity c where c.tiendaId = :tiendaId and c.estado = :estado")
    Optional<CajaSesionEntity> findByTiendaIdAndEstadoConBloqueo(
            @Param("tiendaId") Long tiendaId, @Param("estado") EstadoCajaSesion estado);

    Optional<CajaSesionEntity> findByTiendaIdAndCorrelationIdApertura(Long tiendaId, String correlationIdApertura);

    Optional<CajaSesionEntity> findByTiendaIdAndCorrelationIdCierre(Long tiendaId, String correlationIdCierre);

    List<CajaSesionEntity> findByTiendaId(Long tiendaId);

    Page<CajaSesionEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
