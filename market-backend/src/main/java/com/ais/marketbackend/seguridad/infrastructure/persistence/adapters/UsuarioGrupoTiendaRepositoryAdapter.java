package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioGrupoTiendaRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.RolEntity;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.UsuarioGrupoTiendaEntity;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.RolEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.RolJpaRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.UsuarioGrupoTiendaJpaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional} en las lecturas: {@code RolEntity.permisos} es {@code LAZY}
 * y {@code rolMapper.toDomain} lo recorre — necesita una sesión abierta. Mismo patrón
 * que {@code UsuarioTiendaRepositoryAdapter}.
 */
@Component
public class UsuarioGrupoTiendaRepositoryAdapter implements UsuarioGrupoTiendaRepository {

    private final UsuarioGrupoTiendaJpaRepository jpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final RolEntityMapper rolMapper;

    public UsuarioGrupoTiendaRepositoryAdapter(
            UsuarioGrupoTiendaJpaRepository jpaRepository, RolJpaRepository rolJpaRepository,
            RolEntityMapper rolMapper) {
        this.jpaRepository = jpaRepository;
        this.rolJpaRepository = rolJpaRepository;
        this.rolMapper = rolMapper;
    }

    @Override
    @Transactional
    public UsuarioGrupoTienda save(UsuarioGrupoTienda usuarioGrupoTienda) {
        RolEntity rolEntity = rolJpaRepository.findById(usuarioGrupoTienda.getRol().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol no encontrado: " + usuarioGrupoTienda.getRol().getId()));
        UsuarioGrupoTiendaEntity entity = new UsuarioGrupoTiendaEntity(
                usuarioGrupoTienda.getId(), usuarioGrupoTienda.getUsuarioId(),
                usuarioGrupoTienda.getGrupoTiendaId(), rolEntity);
        UsuarioGrupoTiendaEntity guardado = jpaRepository.save(entity);
        return toDomain(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioGrupoTienda> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream().map(this::toDomain).toList();
    }

    private UsuarioGrupoTienda toDomain(UsuarioGrupoTiendaEntity entity) {
        Rol rol = rolMapper.toDomain(entity.getRol());
        return new UsuarioGrupoTienda(entity.getId(), entity.getUsuarioId(), entity.getGrupoTiendaId(), rol);
    }
}
