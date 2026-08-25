package com.ais.marketbackend.inventario.domain.repository;

import com.ais.marketbackend.inventario.domain.model.MovimientoInventario;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;

/**
 * Solo expone {@code registrar} (insert): el kardex es append-only, no hay
 * {@code update}/{@code delete} — reforzado además por un trigger en PostgreSQL.
 */
public interface MovimientoInventarioRepository {

    MovimientoInventario registrar(MovimientoInventario movimiento);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<MovimientoInventario> findByTiendaIdAndProductoIdOrderByFechaDesc(Long tiendaId, Long productoId);

    Pagina<MovimientoInventario> findByTiendaIdAndProductoIdOrderByFechaDesc(
            Long tiendaId, Long productoId, int pagina, int tamano);
}
