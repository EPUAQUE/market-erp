package com.ais.marketbackend.traslados.infrastructure.persistence.repositories;

import com.ais.marketbackend.traslados.infrastructure.persistence.entities.TrasladoEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrasladoJpaRepository extends JpaRepository<TrasladoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TrasladoEntity t where t.id = :id")
    Optional<TrasladoEntity> findByIdConBloqueo(@Param("id") Long id);

    Page<TrasladoEntity> findByTiendaOrigenIdInOrTiendaDestinoIdIn(
            Collection<Long> tiendaOrigenIds, Collection<Long> tiendaDestinoIds, Pageable pageable);
}
