package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.PermisoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermisoJpaRepository extends JpaRepository<PermisoEntity, Long> {

    boolean existsByCodigo(String codigo);

    @Query("select p.codigo from PermisoEntity p")
    List<String> findAllCodigos();
}
