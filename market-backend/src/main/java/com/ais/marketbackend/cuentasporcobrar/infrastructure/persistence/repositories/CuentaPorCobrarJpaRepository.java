package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.repositories;

import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities.CuentaPorCobrarEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaPorCobrarJpaRepository extends JpaRepository<CuentaPorCobrarEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaPorCobrarEntity c where c.id = :id")
    Optional<CuentaPorCobrarEntity> findByIdConBloqueo(@Param("id") Long id);

    List<CuentaPorCobrarEntity> findByTiendaId(Long tiendaId);

    Page<CuentaPorCobrarEntity> findByTiendaId(Long tiendaId, Pageable pageable);

    Optional<CuentaPorCobrarEntity> findByVentaId(Long ventaId);
}
