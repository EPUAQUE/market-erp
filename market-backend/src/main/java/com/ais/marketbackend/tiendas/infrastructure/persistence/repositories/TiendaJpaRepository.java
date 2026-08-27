package com.ais.marketbackend.tiendas.infrastructure.persistence.repositories;

import com.ais.marketbackend.tiendas.infrastructure.persistence.entities.TiendaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaJpaRepository extends JpaRepository<TiendaEntity, Long> {

    boolean existsByCodigo(String codigo);

    List<TiendaEntity> findByGrupoId(Long grupoId);
}
