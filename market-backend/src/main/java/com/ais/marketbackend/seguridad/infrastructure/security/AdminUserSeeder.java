package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.UsernameCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Conveniencia de desarrollo/local: crea un usuario ADMIN si el catálogo de
 * usuarios está vacío. Se asigna a la tienda {@code 1} (sembrada como "CENTRAL"
 * por el módulo Tiendas en {@code tiendas/001-tienda.xml}) — el rol ADMIN es de
 * alcance global, así que esa asignación solo existe para que el usuario tenga al
 * menos una fila de la que resolver permisos (ver
 * {@code PermisosEfectivosResolverImpl}). Desactivable con
 * {@code app.seed.enabled=false} (obligatorio en producción).
 */
@Component
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);
    private static final long TIENDA_CENTRAL_ID = 1L;

    private final SeedProperties seedProperties;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioService usuarioService;

    public AdminUserSeeder(
            SeedProperties seedProperties,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioService usuarioService) {
        this.seedProperties = seedProperties;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }
        String usernameCanonico = UsernameCanonicalizer.canonicalizar(seedProperties.adminUsername());
        if (usuarioRepository.existsByUsername(usernameCanonico)) {
            return;
        }

        var admin = usuarioService.crear(
                usernameCanonico, seedProperties.adminPassword(), "Administrador", null, null);
        var rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado; revisar migraciones."));
        usuarioService.asignarTiendaSistema(admin.id(), TIENDA_CENTRAL_ID, rolAdmin.getId());

        log.warn("Usuario ADMIN de desarrollo creado: {}. Deshabilite app.seed.enabled en producción.", usernameCanonico);
    }
}
