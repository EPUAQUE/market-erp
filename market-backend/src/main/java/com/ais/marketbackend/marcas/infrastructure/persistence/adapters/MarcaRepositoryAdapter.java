package com.ais.marketbackend.marcas.infrastructure.persistence.adapters;

import com.ais.marketbackend.marcas.domain.model.Marca;
import com.ais.marketbackend.marcas.domain.repository.MarcaRepository;
import com.ais.marketbackend.marcas.infrastructure.persistence.mappers.MarcaEntityMapper;
import com.ais.marketbackend.marcas.infrastructure.persistence.repositories.MarcaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MarcaRepositoryAdapter implements MarcaRepository {

    private final MarcaJpaRepository jpaRepository;
    private final MarcaEntityMapper mapper;

    public MarcaRepositoryAdapter(MarcaJpaRepository jpaRepository, MarcaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Marca save(Marca marca) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(marca)));
    }

    @Override
    public Optional<Marca> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }

    @Override
    public List<Marca> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
