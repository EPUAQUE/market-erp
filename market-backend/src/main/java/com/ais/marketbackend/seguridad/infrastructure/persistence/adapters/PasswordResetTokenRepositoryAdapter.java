package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.model.PasswordResetToken;
import com.ais.marketbackend.seguridad.domain.repository.PasswordResetTokenRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.mappers.PasswordResetTokenEntityMapper;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.PasswordResetTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenEntityMapper mapper;

    public PasswordResetTokenRepositoryAdapter(
            PasswordResetTokenJpaRepository jpaRepository, PasswordResetTokenEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void invalidarNoUsadosDeUsuario(Long usuarioId) {
        jpaRepository.invalidarNoUsadosDeUsuario(usuarioId);
    }

    @Override
    public int consumir(String tokenHash, Instant ahora) {
        return jpaRepository.consumir(tokenHash, ahora);
    }
}
