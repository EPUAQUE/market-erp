package com.ais.marketbackend.tiendas.domain.repository;

import com.ais.marketbackend.tiendas.domain.model.Tienda;
import java.util.List;
import java.util.Optional;

public interface TiendaRepository {

    Tienda save(Tienda tienda);

    Optional<Tienda> findById(Long id);

    boolean existsByCodigo(String codigo);

    List<Tienda> findAll();

    /** Usado por Seguridad para resolver el alcance de una asignación de grupo completo. */
    List<Long> listarIdsPorGrupo(Long grupoId);
}
