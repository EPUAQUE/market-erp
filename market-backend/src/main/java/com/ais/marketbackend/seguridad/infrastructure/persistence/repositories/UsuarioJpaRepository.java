package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.UsuarioEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where u.id = :id")
    Optional<UsuarioEntity> findByIdConBloqueo(@Param("id") Long id);
}
