package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.RolEntity;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.UsuarioTiendaEntity;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.RolEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.RolJpaRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.UsuarioTiendaJpaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional} en las lecturas: {@code RolEntity.permisos} es {@code LAZY}
 * y {@code rolMapper.toDomain} lo recorre — necesita una sesión abierta.
 */
@Component
public class UsuarioTiendaRepositoryAdapter implements UsuarioTiendaRepository {

    private final UsuarioTiendaJpaRepository jpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final RolEntityMapper rolMapper;

    public UsuarioTiendaRepositoryAdapter(
            UsuarioTiendaJpaRepository jpaRepository, RolJpaRepository rolJpaRepository, RolEntityMapper rolMapper) {
        this.jpaRepository = jpaRepository;
        this.rolJpaRepository = rolJpaRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    @Transactional
    public UsuarioTienda save(UsuarioTienda usuarioTienda) {
        RolEntity rolEntity = rolJpaRepository.findById(usuarioTienda.getRol().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + usuarioTienda.getRol().getId()));
        UsuarioTiendaEntity entity = new UsuarioTiendaEntity(
                usuarioTienda.getId(), usuarioTienda.getUsuarioId(), usuarioTienda.getTiendaId(), rolEntity);
        UsuarioTiendaEntity guardado = jpaRepository.save(entity);
        return toDomain(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioTienda> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream().map(this::toDomain).toList();
    }

    private UsuarioTienda toDomain(UsuarioTiendaEntity entity) {
        Rol rol = rolMapper.toDomain(entity.getRol());
        return new UsuarioTienda(entity.getId(), entity.getUsuarioId(), entity.getTiendaId(), rol);
    }
}
