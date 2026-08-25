package com.ais.marketbackend.inventario.domain.repository;

import com.ais.marketbackend.inventario.domain.model.Inventario;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface InventarioRepository {

    Inventario save(Inventario inventario);

    Optional<Inventario> findByTiendaIdAndProductoId(Long tiendaId, Long productoId);

    /**
     * Igual que {@link #findByTiendaIdAndProductoId}, pero con bloqueo pesimista de
     * escritura ({@code PESSIMISTIC_WRITE}) — uso exclusivo del camino de mutación
     * ({@code InventarioServiceImpl.registrarMovimiento}) para serializar movimientos
     * concurrentes sobre la misma fila. Las lecturas puras (ej. {@code obtener()})
     * deben seguir usando la variante sin bloqueo.
     */
    Optional<Inventario> findByTiendaIdAndProductoIdConBloqueo(Long tiendaId, Long productoId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<Inventario> findByTiendaId(Long tiendaId);

    Pagina<Inventario> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
