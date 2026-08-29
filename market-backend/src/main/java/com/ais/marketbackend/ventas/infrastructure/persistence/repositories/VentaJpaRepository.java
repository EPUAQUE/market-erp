package com.ais.marketbackend.ventas.infrastructure.persistence.repositories;

import com.ais.marketbackend.ventas.infrastructure.persistence.entities.VentaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaJpaRepository extends JpaRepository<VentaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VentaEntity v where v.id = :id")
    Optional<VentaEntity> findByIdConBloqueo(@Param("id") Long id);

    List<VentaEntity> findByTiendaId(Long tiendaId);

    Page<VentaEntity> findByTiendaId(Long tiendaId, Pageable pageable);

    Optional<VentaEntity> findByTiendaIdAndVendedorIdAndCorrelationId(
            Long tiendaId, Long vendedorId, String correlationId);
}
