package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.RolEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.RolJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional} en cada método: {@code RolEntity.permisos} es {@code LAZY}
 * y el mapper de MapStruct lo recorre al mapear a dominio — sin una sesión abierta
 * eso lanza {@code LazyInitializationException} en cuanto el caller no está, a su
 * vez, en una transacción (ej. un {@code ApplicationRunner} de arranque).
 */
@Component
public class RolRepositoryAdapter implements RolRepository {

    private final RolJpaRepository jpaRepository;
    private final RolEntityMapper mapper;

    public RolRepositoryAdapter(RolJpaRepository jpaRepository, RolEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> findByNombre(String nombre) {
        return jpaRepository.findByNombre(nombre).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rol> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
