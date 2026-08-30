package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.shared.responses.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Bloquea el resto de la API mientras el claim {@code debeCambiarPassword} del
 * access token esté activo, salvo las rutas que permiten cumplirlo o cerrar la
 * sesión. El claim queda fijo desde el login: cualquier cambio real de la
 * marca ({@code Usuario.cambiarPassword}/{@code restablecerConPasswordTemporal})
 * también incrementa {@code versionSeguridad}, así que un token con la marca
 * vieja ya es invalidado por {@link SecurityVersionValidator} antes de llegar
 * aquí — ver seguridad-desarrolladores.md §5.
 */
public class DebeCambiarPasswordFilter extends OncePerRequestFilter {

    private static final Set<String> RUTAS_PERMITIDAS =
            Set.of("/api/v1/auth/password", "/api/v1/auth/logout", "/api/v1/auth/me");

    private final ObjectMapper objectMapper;

    public DebeCambiarPasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof Jwt jwt
                && Boolean.TRUE.equals(jwt.getClaimAsBoolean("debeCambiarPassword"))
                && !RUTAS_PERMITIDAS.contains(request.getRequestURI())) {
            escribirError(response, request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void escribirError(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "DEBE_CAMBIAR_PASSWORD",
                "Debe cambiar su contraseña antes de continuar.", path, UUID.randomUUID().toString());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
