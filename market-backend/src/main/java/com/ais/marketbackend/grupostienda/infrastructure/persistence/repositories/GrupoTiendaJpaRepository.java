package com.ais.marketbackend.grupostienda.infrastructure.persistence.repositories;

import com.ais.marketbackend.grupostienda.infrastructure.persistence.entities.GrupoTiendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoTiendaJpaRepository extends JpaRepository<GrupoTiendaEntity, Long> {

    boolean existsByCodigo(String codigo);
}
