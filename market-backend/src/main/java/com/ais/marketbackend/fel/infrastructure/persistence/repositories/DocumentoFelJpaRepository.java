package com.ais.marketbackend.fel.infrastructure.persistence.repositories;

import com.ais.marketbackend.fel.infrastructure.persistence.entities.DocumentoFelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoFelJpaRepository extends JpaRepository<DocumentoFelEntity, Long> {

    Optional<DocumentoFelEntity> findByVentaId(Long ventaId);

    List<DocumentoFelEntity> findByTiendaId(Long tiendaId);

    @Query("select coalesce(max(d.numero), 0) from DocumentoFelEntity d where d.tiendaId = :tiendaId and d.serie = :serie")
    long findMaxNumero(@Param("tiendaId") Long tiendaId, @Param("serie") String serie);
}
