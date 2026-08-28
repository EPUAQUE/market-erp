package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.UsuarioTiendaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioTiendaJpaRepository extends JpaRepository<UsuarioTiendaEntity, Long> {

    List<UsuarioTiendaEntity> findByUsuarioId(Long usuarioId);

    List<UsuarioTiendaEntity> findByTiendaIdIn(Collection<Long> tiendaIds);
}
