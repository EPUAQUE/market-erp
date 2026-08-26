package com.ais.marketbackend.compras.infrastructure.persistence.repositories;

import com.ais.marketbackend.compras.infrastructure.persistence.entities.CompraEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraJpaRepository extends JpaRepository<CompraEntity, Long> {

    List<CompraEntity> findByTiendaId(Long tiendaId);

    Page<CompraEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
