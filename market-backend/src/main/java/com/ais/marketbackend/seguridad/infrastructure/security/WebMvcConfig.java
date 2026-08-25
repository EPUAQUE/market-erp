package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UsuarioRepository usuarioRepository;
    private final PermisosEfectivosResolver permisosEfectivosResolver;

    public WebMvcConfig(UsuarioRepository usuarioRepository, PermisosEfectivosResolver permisosEfectivosResolver) {
        this.usuarioRepository = usuarioRepository;
        this.permisosEfectivosResolver = permisosEfectivosResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PermissionInterceptor(usuarioRepository, permisosEfectivosResolver))
                .excludePathPatterns("/api/v1/auth/**", "/actuator/**");
    }
}
