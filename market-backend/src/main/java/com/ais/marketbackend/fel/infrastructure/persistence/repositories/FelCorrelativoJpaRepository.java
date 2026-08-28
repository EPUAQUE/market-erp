package com.ais.marketbackend.fel.infrastructure.persistence.repositories;

import com.ais.marketbackend.fel.infrastructure.persistence.entities.FelCorrelativoEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FelCorrelativoJpaRepository extends JpaRepository<FelCorrelativoEntity, Long> {

    /**
     * {@code @Query} explícito porque el sufijo "ConBloqueo" no es una propiedad de
     * {@code FelCorrelativoEntity} — con derivación automática del nombre, Spring Data
     * intenta resolverlo como parte del path de propiedades y falla en el arranque.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FelCorrelativoEntity c where c.tiendaId = :tiendaId and c.serie = :serie")
    Optional<FelCorrelativoEntity> findByTiendaIdAndSerieConBloqueo(
            @Param("tiendaId") Long tiendaId, @Param("serie") String serie);
}
