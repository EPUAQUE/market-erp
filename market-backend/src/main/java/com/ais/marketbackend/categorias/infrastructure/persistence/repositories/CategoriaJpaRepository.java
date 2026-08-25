package com.ais.marketbackend.categorias.infrastructure.persistence.repositories;

import com.ais.marketbackend.categorias.infrastructure.persistence.entities.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    boolean existsByNombre(String nombre);
}
