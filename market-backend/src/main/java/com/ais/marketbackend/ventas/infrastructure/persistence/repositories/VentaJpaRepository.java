package com.ais.marketbackend.ventas.infrastructure.persistence.repositories;

import com.ais.marketbackend.ventas.infrastructure.persistence.entities.VentaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaJpaRepository extends JpaRepository<VentaEntity, Long> {

    List<VentaEntity> findByTiendaId(Long tiendaId);

    Page<VentaEntity> findByTiendaId(Long tiendaId, Pageable pageable);

    Optional<VentaEntity> findByTiendaIdAndVendedorIdAndCorrelationId(
            Long tiendaId, Long vendedorId, String correlationId);
}
