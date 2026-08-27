package com.ais.marketbackend.grupostienda.domain.repository;

import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import java.util.List;
import java.util.Optional;

public interface GrupoTiendaRepository {

    GrupoTienda save(GrupoTienda grupoTienda);

    Optional<GrupoTienda> findById(Long id);

    boolean existsByCodigo(String codigo);

    List<GrupoTienda> findAll();
}
