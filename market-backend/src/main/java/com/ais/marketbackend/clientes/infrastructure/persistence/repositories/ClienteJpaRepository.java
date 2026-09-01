package com.ais.marketbackend.clientes.infrastructure.persistence.repositories;

import com.ais.marketbackend.clientes.infrastructure.persistence.entities.ClienteEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    boolean existsByNit(String nit);

    Optional<ClienteEntity> findByCorrelationId(String correlationId);

    Optional<ClienteEntity> findByNombre(String nombre);

    /**
     * {@code @Query} explícito (no {@code @Lock} sobre {@code findById}
     * heredado) — mismo motivo que {@code InventarioJpaRepository}: Spring
     * Data no permite anotar con {@code @Lock} un método heredado de
     * {@code CrudRepository} directamente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClienteEntity c where c.id = :id")
    Optional<ClienteEntity> findByIdConBloqueo(@Param("id") Long id);
}
