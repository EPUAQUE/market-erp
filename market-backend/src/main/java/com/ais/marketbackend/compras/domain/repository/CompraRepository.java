package com.ais.marketbackend.compras.domain.repository;

import com.ais.marketbackend.compras.domain.model.Compra;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CompraRepository {

    Compra save(Compra compra);

    Optional<Compra> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code CompraServiceImpl.recibir}/{@code anular} para serializar
     * transiciones de estado concurrentes sobre la misma compra. Sin esto, dos
     * llamadas casi simultáneas (p. ej. dos {@code recibir}, o un
     * {@code recibir} contra un {@code anular}) podían leer el mismo estado
     * BORRADOR y ambas pasar la validación — duplicando el movimiento de
     * Inventario y la creación de la cuenta por pagar, o dejando la compra en
     * un estado final que no refleja qué transición "ganó" en realidad.
     */
    Optional<Compra> findByIdConBloqueo(Long id);

    List<Compra> findByTiendaId(Long tiendaId);

    Pagina<Compra> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
