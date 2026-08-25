package com.ais.marketbackend.unidadesmedida.infrastructure.persistence.adapters;

import com.ais.marketbackend.unidadesmedida.domain.model.UnidadMedida;
import com.ais.marketbackend.unidadesmedida.domain.repository.UnidadMedidaRepository;
import com.ais.marketbackend.unidadesmedida.infrastructure.persistence.mappers.UnidadMedidaEntityMapper;
import com.ais.marketbackend.unidadesmedida.infrastructure.persistence.repositories.UnidadMedidaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnidadMedidaRepositoryAdapter implements UnidadMedidaRepository {

    private final UnidadMedidaJpaRepository jpaRepository;
    private final UnidadMedidaEntityMapper mapper;

    public UnidadMedidaRepositoryAdapter(UnidadMedidaJpaRepository jpaRepository, UnidadMedidaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UnidadMedida save(UnidadMedida unidadMedida) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(unidadMedida)));
    }

    @Override
    public Optional<UnidadMedida> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }

    @Override
    public List<UnidadMedida> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
