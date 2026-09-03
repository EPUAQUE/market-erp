package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.PasswordResetTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    void invalidarNoUsadosDeUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usado = true "
            + "where t.tokenHash = :tokenHash and t.usado = false and t.expiraEn > :ahora")
    int consumir(@Param("tokenHash") String tokenHash, @Param("ahora") Instant ahora);
}
