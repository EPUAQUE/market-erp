package com.ais.marketbackend.marcas.infrastructure.persistence.repositories;

import com.ais.marketbackend.marcas.infrastructure.persistence.entities.MarcaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaJpaRepository extends JpaRepository<MarcaEntity, Long> {

    boolean existsByNombre(String nombre);
}
