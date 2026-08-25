package com.ais.marketbackend.clientes.infrastructure.persistence.repositories;

import com.ais.marketbackend.clientes.infrastructure.persistence.entities.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    boolean existsByNit(String nit);
}
