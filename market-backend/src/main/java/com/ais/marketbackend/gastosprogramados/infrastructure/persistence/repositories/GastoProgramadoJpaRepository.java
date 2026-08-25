package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.repositories;

import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities.GastoProgramadoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoProgramadoJpaRepository extends JpaRepository<GastoProgramadoEntity, Long> {

    List<GastoProgramadoEntity> findByTiendaId(Long tiendaId);
}
