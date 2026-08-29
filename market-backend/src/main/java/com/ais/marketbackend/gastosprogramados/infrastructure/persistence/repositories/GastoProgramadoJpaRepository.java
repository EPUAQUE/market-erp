package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.repositories;

import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities.GastoProgramadoEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoProgramadoJpaRepository extends JpaRepository<GastoProgramadoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GastoProgramadoEntity g where g.id = :id")
    Optional<GastoProgramadoEntity> findByIdConBloqueo(@Param("id") Long id);

    List<GastoProgramadoEntity> findByTiendaId(Long tiendaId);
}
