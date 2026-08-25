package com.ais.marketbackend.traslados.infrastructure.persistence.repositories;

import com.ais.marketbackend.traslados.infrastructure.persistence.entities.TrasladoEntity;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrasladoJpaRepository extends JpaRepository<TrasladoEntity, Long> {

    Page<TrasladoEntity> findByTiendaOrigenIdInOrTiendaDestinoIdIn(
            Collection<Long> tiendaOrigenIds, Collection<Long> tiendaDestinoIds, Pageable pageable);
}
