package com.ais.marketbackend.unidadesmedida.infrastructure.persistence.repositories;

import com.ais.marketbackend.unidadesmedida.infrastructure.persistence.entities.UnidadMedidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaJpaRepository extends JpaRepository<UnidadMedidaEntity, Long> {

    boolean existsByNombre(String nombre);
}
