package com.ais.marketbackend.traslados.domain.repository;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TrasladoRepository {

    Traslado save(Traslado traslado);

    Optional<Traslado> findById(Long id);

    /** Sin paginar — uso interno. El endpoint público usa las variantes paginadas de abajo. */
    List<Traslado> findAll();

    Pagina<Traslado> listar(int pagina, int tamano);

    /** Filtra a nivel de BD por tienda de origen O destino — necesario para paginar correctamente un alcance por tienda. */
    Pagina<Traslado> listarPorTiendas(Set<Long> tiendaIds, int pagina, int tamano);
}
