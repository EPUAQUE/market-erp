package com.ais.marketbackend.proveedores.infrastructure.persistence.repositories;

import com.ais.marketbackend.proveedores.infrastructure.persistence.entities.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorEntity, Long> {

    boolean existsByNit(String nit);
}
