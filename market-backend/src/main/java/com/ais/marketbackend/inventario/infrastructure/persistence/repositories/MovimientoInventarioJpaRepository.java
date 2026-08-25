package com.ais.marketbackend.inventario.infrastructure.persistence.repositories;

import com.ais.marketbackend.inventario.infrastructure.persistence.entities.MovimientoInventarioEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioJpaRepository extends JpaRepository<MovimientoInventarioEntity, Long> {

    List<MovimientoInventarioEntity> findByTiendaIdAndProductoIdOrderByFechaDesc(Long tiendaId, Long productoId);

    Page<MovimientoInventarioEntity> findByTiendaIdAndProductoIdOrderByFechaDesc(
            Long tiendaId, Long productoId, Pageable pageable);
}
