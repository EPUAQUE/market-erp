package com.ais.marketbackend.traslados.domain.repository;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TrasladoRepository {

    Traslado save(Traslado traslado);

    Optional<Traslado> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code TrasladoServiceImpl.completar}/{@code anular} para serializar
     * transiciones de estado concurrentes sobre el mismo traslado. Sin esto,
     * dos {@code completar} casi simultáneos podían leer el mismo estado
     * BORRADOR y ambos pasar la validación, duplicando los movimientos de
     * Inventario (salida en origen y entrada en destino) sin ninguna
     * restricción de BD que lo impida.
     */
    Optional<Traslado> findByIdConBloqueo(Long id);

    /** Sin paginar — uso interno. El endpoint público usa las variantes paginadas de abajo. */
    List<Traslado> findAll();

    Pagina<Traslado> listar(int pagina, int tamano);

    /** Filtra a nivel de BD por tienda de origen O destino — necesario para paginar correctamente un alcance por tienda. */
    Pagina<Traslado> listarPorTiendas(Set<Long> tiendaIds, int pagina, int tamano);
}
