package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.repositories;

import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities.CuentaPorPagarEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaPorPagarJpaRepository extends JpaRepository<CuentaPorPagarEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaPorPagarEntity c where c.id = :id")
    Optional<CuentaPorPagarEntity> findByIdConBloqueo(@Param("id") Long id);

    List<CuentaPorPagarEntity> findByTiendaId(Long tiendaId);

    Page<CuentaPorPagarEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
