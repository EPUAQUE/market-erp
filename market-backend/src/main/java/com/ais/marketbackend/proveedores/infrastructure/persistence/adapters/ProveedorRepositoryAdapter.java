package com.ais.marketbackend.proveedores.infrastructure.persistence.adapters;

import com.ais.marketbackend.proveedores.domain.model.Proveedor;
import com.ais.marketbackend.proveedores.domain.repository.ProveedorRepository;
import com.ais.marketbackend.proveedores.infrastructure.persistence.mappers.ProveedorEntityMapper;
import com.ais.marketbackend.proveedores.infrastructure.persistence.repositories.ProveedorJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProveedorRepositoryAdapter implements ProveedorRepository {

    private final ProveedorJpaRepository jpaRepository;
    private final ProveedorEntityMapper mapper;

    public ProveedorRepositoryAdapter(ProveedorJpaRepository jpaRepository, ProveedorEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Proveedor save(Proveedor proveedor) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(proveedor)));
    }

    @Override
    public Optional<Proveedor> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNit(String nit) {
        return jpaRepository.existsByNit(nit);
    }

    @Override
    public List<Proveedor> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
