package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.RefreshToken;
import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.RefreshTokenEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenEntityMapper mapper;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository, RefreshTokenEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(refreshToken)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findChain(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void revocarTodosDeUsuario(Long usuarioId) {
        jpaRepository.revocarTodosDeUsuario(usuarioId);
    }

    @Override
    public int consumir(String tokenHash, Instant ahora) {
        return jpaRepository.consumir(tokenHash, ahora);
    }

    @Override
    public int eliminarExpirados(Instant antesDe) {
        return jpaRepository.eliminarExpirados(antesDe);
    }
}
