package com.ais.marketbackend.categorias.infrastructure.persistence.adapters;

import com.ais.marketbackend.categorias.domain.model.Categoria;
import com.ais.marketbackend.categorias.domain.repository.CategoriaRepository;
import com.ais.marketbackend.categorias.infrastructure.persistence.mappers.CategoriaEntityMapper;
import com.ais.marketbackend.categorias.infrastructure.persistence.repositories.CategoriaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CategoriaRepositoryAdapter implements CategoriaRepository {

    private final CategoriaJpaRepository jpaRepository;
    private final CategoriaEntityMapper mapper;

    public CategoriaRepositoryAdapter(CategoriaJpaRepository jpaRepository, CategoriaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Categoria save(Categoria categoria) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(categoria)));
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }

    @Override
    public List<Categoria> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
