package com.ais.marketbackend.compras.infrastructure.persistence.repositories;

import com.ais.marketbackend.compras.infrastructure.persistence.entities.CompraEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraJpaRepository extends JpaRepository<CompraEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompraEntity c where c.id = :id")
    Optional<CompraEntity> findByIdConBloqueo(@Param("id") Long id);

    List<CompraEntity> findByTiendaId(Long tiendaId);

    Page<CompraEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
