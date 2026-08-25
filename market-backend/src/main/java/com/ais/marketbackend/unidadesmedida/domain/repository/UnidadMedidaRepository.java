package com.ais.marketbackend.unidadesmedida.domain.repository;

import com.ais.marketbackend.unidadesmedida.domain.model.UnidadMedida;
import java.util.List;
import java.util.Optional;

public interface UnidadMedidaRepository {

    UnidadMedida save(UnidadMedida unidadMedida);

    Optional<UnidadMedida> findById(Long id);

    boolean existsByNombre(String nombre);

    List<UnidadMedida> findAll();
}
