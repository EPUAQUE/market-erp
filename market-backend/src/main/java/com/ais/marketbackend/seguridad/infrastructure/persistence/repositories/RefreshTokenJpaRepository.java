package com.ais.marketbackend.seguridad.infrastructure.persistence.repositories;

import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.RefreshTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByUsuarioId(Long usuarioId);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revocado = true where r.usuarioId = :usuarioId and r.revocado = false")
    void revocarTodosDeUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revocado = true "
            + "where r.tokenHash = :tokenHash and r.revocado = false and r.expiraEn > :ahora")
    int consumir(@Param("tokenHash") String tokenHash, @Param("ahora") Instant ahora);

    @Modifying
    @Query("delete from RefreshTokenEntity r where r.expiraEn < :antesDe")
    int eliminarExpirados(@Param("antesDe") Instant antesDe);
}
