package com.ais.marketbackend.marcas.domain.repository;

import com.ais.marketbackend.marcas.domain.model.Marca;
import java.util.List;
import java.util.Optional;

public interface MarcaRepository {

    Marca save(Marca marca);

    Optional<Marca> findById(Long id);

    boolean existsByNombre(String nombre);

    List<Marca> findAll();
}
