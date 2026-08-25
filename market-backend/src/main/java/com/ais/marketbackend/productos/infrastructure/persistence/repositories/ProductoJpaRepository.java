package com.ais.marketbackend.productos.infrastructure.persistence.repositories;

import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    boolean existsByCodigoInterno(String codigoInterno);
}
