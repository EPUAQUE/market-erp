package com.ais.marketbackend.inventario.infrastructure.persistence.repositories;

import com.ais.marketbackend.inventario.infrastructure.persistence.entities.InventarioEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventarioJpaRepository extends JpaRepository<InventarioEntity, Long> {

    Optional<InventarioEntity> findByTiendaIdAndProductoId(Long tiendaId, Long productoId);

    /**
     * {@code @Query} explícito porque el sufijo "ConBloqueo" no es una propiedad de
     * {@code InventarioEntity} — con derivación automática del nombre, Spring Data
     * intenta resolverlo como parte del path de propiedades y falla en el arranque.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventarioEntity i where i.tiendaId = :tiendaId and i.productoId = :productoId")
    Optional<InventarioEntity> findByTiendaIdAndProductoIdConBloqueo(
            @Param("tiendaId") Long tiendaId, @Param("productoId") Long productoId);

    List<InventarioEntity> findByTiendaId(Long tiendaId);

    Page<InventarioEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
