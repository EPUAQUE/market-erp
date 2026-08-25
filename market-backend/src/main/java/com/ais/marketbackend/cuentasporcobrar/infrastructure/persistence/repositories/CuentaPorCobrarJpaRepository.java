package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.repositories;

import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities.CuentaPorCobrarEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaPorCobrarJpaRepository extends JpaRepository<CuentaPorCobrarEntity, Long> {

    List<CuentaPorCobrarEntity> findByTiendaId(Long tiendaId);

    Page<CuentaPorCobrarEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
