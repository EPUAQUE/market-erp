package com.ais.marketbackend.productos.infrastructure.persistence.repositories;

import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoTiendaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoTiendaJpaRepository extends JpaRepository<ProductoTiendaEntity, Long> {

    Optional<ProductoTiendaEntity> findByProductoIdAndTiendaId(Long productoId, Long tiendaId);

    List<ProductoTiendaEntity> findByProductoId(Long productoId);

    List<ProductoTiendaEntity> findByTiendaId(Long tiendaId);

    Page<ProductoTiendaEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
