package com.ais.marketbackend.fel.infrastructure.persistence.repositories;

import com.ais.marketbackend.fel.infrastructure.persistence.entities.DocumentoFelEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoFelJpaRepository extends JpaRepository<DocumentoFelEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentoFelEntity d where d.id = :id")
    Optional<DocumentoFelEntity> findByIdConBloqueo(@Param("id") Long id);

    Optional<DocumentoFelEntity> findByVentaId(Long ventaId);

    List<DocumentoFelEntity> findByTiendaId(Long tiendaId);

    Page<DocumentoFelEntity> findByTiendaId(Long tiendaId, Pageable pageable);

    @Query("select coalesce(max(d.numero), 0) from DocumentoFelEntity d where d.tiendaId = :tiendaId and d.serie = :serie")
    long findMaxNumero(@Param("tiendaId") Long tiendaId, @Param("serie") String serie);
}
