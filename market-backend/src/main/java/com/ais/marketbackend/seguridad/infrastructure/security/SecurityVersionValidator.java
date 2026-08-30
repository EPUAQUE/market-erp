package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Revalida por petición que el claim {@code sver} del access token siga
 * coincidiendo con {@code Usuario.versionSeguridad} en base de datos — un
 * cambio de contraseña, bloqueo/desactivación o revocación administrativa de
 * sesiones incrementa esa versión e invalida así de inmediato cualquier access
 * token emitido antes, sin esperar su expiración natural. Cierra el hueco
 * documentado en seguridad-desarrolladores.md §5 ("invalidación temprana,
 * aspiracional, no implementada").
 */
@Component
public class SecurityVersionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESION_INVALIDA =
            new OAuth2Error("invalid_token", "La sesión ya no es válida.", null);

    private final UsuarioRepository usuarioRepository;

    public SecurityVersionValidator(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Number tokenVersion = token.getClaim("sver");
        if (tokenVersion == null) {
            return OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
        }

        Optional<Usuario> usuario = usuarioRepository.findByUsername(token.getSubject());
        boolean vigente = usuario.isPresent()
                && usuario.get().estaActivo()
                && usuario.get().getVersionSeguridad() == tokenVersion.longValue();
        return vigente ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESION_INVALIDA);
    }
}
