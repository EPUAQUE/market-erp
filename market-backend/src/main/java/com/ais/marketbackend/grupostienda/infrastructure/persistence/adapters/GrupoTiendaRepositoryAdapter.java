package com.ais.marketbackend.grupostienda.infrastructure.persistence.adapters;

import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.grupostienda.infrastructure.persistence.mappers.GrupoTiendaEntityMapper;
import com.ais.marketbackend.grupostienda.infrastructure.persistence.repositories.GrupoTiendaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GrupoTiendaRepositoryAdapter implements GrupoTiendaRepository {

    private final GrupoTiendaJpaRepository jpaRepository;
    private final GrupoTiendaEntityMapper mapper;

    public GrupoTiendaRepositoryAdapter(GrupoTiendaJpaRepository jpaRepository, GrupoTiendaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GrupoTienda save(GrupoTienda grupoTienda) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(grupoTienda)));
    }

    @Override
    public Optional<GrupoTienda> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpaRepository.existsByCodigo(codigo);
    }

    @Override
    public List<GrupoTienda> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
