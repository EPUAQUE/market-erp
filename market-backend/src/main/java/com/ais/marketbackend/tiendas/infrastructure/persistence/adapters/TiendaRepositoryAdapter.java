package com.ais.marketbackend.tiendas.infrastructure.persistence.adapters;

import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import com.ais.marketbackend.tiendas.infrastructure.persistence.mappers.TiendaEntityMapper;
import com.ais.marketbackend.tiendas.infrastructure.persistence.repositories.TiendaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TiendaRepositoryAdapter implements TiendaRepository {

    private final TiendaJpaRepository jpaRepository;
    private final TiendaEntityMapper mapper;

    public TiendaRepositoryAdapter(TiendaJpaRepository jpaRepository, TiendaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Tienda save(Tienda tienda) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(tienda)));
    }

    @Override
    public Optional<Tienda> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpaRepository.existsByCodigo(codigo);
    }

    @Override
    public List<Tienda> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
