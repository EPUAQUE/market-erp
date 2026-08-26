package com.ais.marketbackend.productos.infrastructure.storage;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve como estático lo que {@link ImagenProductoAlmacenamientoService} guarda en
 * disco — misma carpeta, misma propiedad de configuración. La ruta pública
 * {@code /api/v1/productos/imagenes/**} cae dentro de {@code /api/*}, así que Caddy
 * (ver deploy/Caddyfile) ya la enruta al backend sin cambios adicionales.
 */
@Configuration
public class ImagenesWebConfig implements WebMvcConfigurer {

    private final String directorioConfigurado;

    public ImagenesWebConfig(@Value("${app.storage.productos-imagenes-dir}") String directorioConfigurado) {
        this.directorioConfigurado = directorioConfigurado;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Path.of(directorioConfigurado).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/v1/productos/imagenes/**").addResourceLocations(ubicacion);
    }
}
