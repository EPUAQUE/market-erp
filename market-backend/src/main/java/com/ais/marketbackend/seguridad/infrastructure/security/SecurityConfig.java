package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.shared.infrastructure.web.CorrelationIdFilter;
import com.ais.marketbackend.shared.responses.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SeguridadProperties properties) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/actuator/health").permitAll()
                        // Lectura pública, igual que cualquier otra imagen alojada externamente
                        // (lo que reemplaza) — nunca requirió JWT antes, y <img>/Image.network no
                        // adjuntan el header Authorization. Solo GET: la subida (POST .../imagen)
                        // sigue exigiendo PRODUCTOS_EDITAR vía @RequiresPermission.
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/imagenes/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .headers(headers -> headers
                        .cacheControl(cache -> {})
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)))
                .addFilterAfter(new DebeCambiarPasswordFilter(objectMapper), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Registrado fuera de la cadena de Spring Security a propósito ({@code
     * Ordered.HIGHEST_PRECEDENCE} lo pone antes que el propio {@code
     * FilterChainProxy} de Security en la cadena de servlet) — así hasta un 401 de
     * autenticación o un 429 de rate limit, que nunca llegan a un {@code @RestController},
     * tienen correlationId.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeJsonError(
                response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Credenciales inválidas o sesión no válida.", request.getRequestURI());
    }

    private org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJsonError(
                response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "No tiene permiso para esta operación.", request.getRequestURI());
    }

    private void writeJsonError(
            jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String code, String message,
            String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), code, message, path, UUID.randomUUID().toString());
        objectMapper.writeValue(response.getWriter(), body);
    }

    private CorsConfigurationSource corsConfigurationSource(SeguridadProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(properties.cors().allowedMethods());
        configuration.setAllowedHeaders(properties.cors().allowedHeaders());
        // El refresh token viaja en cookie: hace falta enviar/recibir credenciales
        // entre orígenes (frontend:5173 / backend:8080 en desarrollo). Por eso la
        // allowlist de orígenes nunca puede incluir "*" junto a esta bandera.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
