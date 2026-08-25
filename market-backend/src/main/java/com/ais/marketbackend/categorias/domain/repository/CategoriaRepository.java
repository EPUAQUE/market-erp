package com.ais.marketbackend.categorias.domain.repository;

import com.ais.marketbackend.categorias.domain.model.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

    Categoria save(Categoria categoria);

    Optional<Categoria> findById(Long id);

    boolean existsByNombre(String nombre);

    List<Categoria> findAll();
}
