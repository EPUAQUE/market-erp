package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.UsuarioEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.UsuarioJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "id");

    private final UsuarioJpaRepository jpaRepository;
    private final UsuarioEntityMapper mapper;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpaRepository, UsuarioEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Usuario save(Usuario usuario) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(usuario)));
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public List<Usuario> findAll() {
        return jpaRepository.findAll(MAS_RECIENTE_PRIMERO).stream().map(mapper::toDomain).toList();
    }
}
