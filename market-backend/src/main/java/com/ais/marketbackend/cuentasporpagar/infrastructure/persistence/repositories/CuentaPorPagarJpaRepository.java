package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.repositories;

import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities.CuentaPorPagarEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaPorPagarJpaRepository extends JpaRepository<CuentaPorPagarEntity, Long> {

    List<CuentaPorPagarEntity> findByTiendaId(Long tiendaId);
}
