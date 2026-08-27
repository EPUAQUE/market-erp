package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.UsuarioGrupoTiendaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioGrupoTiendaJpaRepository extends JpaRepository<UsuarioGrupoTiendaEntity, Long> {

    List<UsuarioGrupoTiendaEntity> findByUsuarioId(Long usuarioId);
}
