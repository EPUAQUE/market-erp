package com.ais.marketbackend.compras.infrastructure.persistence.adapters;

import com.ais.marketbackend.compras.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.compras.domain.model.Compra;
import com.ais.marketbackend.compras.domain.repository.CompraRepository;
import com.ais.marketbackend.compras.infrastructure.persistence.mappers.CompraEntityMapper;
import com.ais.marketbackend.compras.infrastructure.persistence.repositories.CompraJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CompraRepositoryAdapter implements CompraRepository {

    private final CompraJpaRepository jpaRepository;
    private final CompraEntityMapper mapper;

    public CompraRepositoryAdapter(CompraJpaRepository jpaRepository, CompraEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Compra save(Compra compra) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(compra)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("El proveedor, la tienda o el producto indicado no existe.");
        }
    }

    @Override
    public Optional<Compra> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Compra> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }
}
