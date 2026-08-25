package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.service.AccessTokenIssuer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * {@code sub} usa el username canónico: es el identificador público y estable ya
 * existente en el modelo, y no expone el id autoincremental de base de datos.
 */
@Component
public class AccessTokenIssuerImpl implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final SeguridadProperties properties;

    public AccessTokenIssuerImpl(JwtEncoder jwtEncoder, SeguridadProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    @Override
    public Resultado emitir(Usuario usuario, PermisosEfectivos permisos) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(properties.jwt().accessTokenTtl());

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .keyId(properties.jwt().activeKid())
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(List.of(properties.jwt().audience()))
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .subject(usuario.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("alcanceGlobal", permisos.alcanceGlobal())
                .claim("tiendas", permisos.tiendaIds())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Resultado(token, expiraEn);
    }
}
