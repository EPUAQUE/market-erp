package com.ais.marketbackend.seguridad.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Rechaza el arranque en el perfil {@code prod} si la configuración resultante es
 * insegura — corre en el constructor (fase de creación de beans, antes de que
 * {@link AdminUserSeeder} u otro {@code ApplicationRunner} ejecute ningún efecto
 * secundario) para fallar lo antes posible.
 *
 * <p>Complementa, no reemplaza, a los {@code ${VAR}} sin default de
 * {@code application-prod.yml}: aquellos cubren "falta la variable", esto cubre
 * "la variable está puesta pero apunta a algo de desarrollo/conocido".
 */
@Component
public class ProdSafetyGuard {

    private static final String CREDENCIAL_ADMIN_CONOCIDA = "Admin1234!Seguro";
    private static final String MARCADOR_CERTIFICADO_DEV = "dev-";

    public ProdSafetyGuard(Environment environment, SeedProperties seedProperties, SeguridadProperties seguridadProperties) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        List<String> problemas = new ArrayList<>();

        if (seedProperties.enabled()) {
            problemas.add("app.seed.enabled=true (SEED_ENABLED) no debe usarse en producción.");
        }
        if (CREDENCIAL_ADMIN_CONOCIDA.equals(seedProperties.adminPassword())) {
            problemas.add("SEED_ADMIN_PASSWORD usa la contraseña de desarrollo conocida por defecto.");
        }
        for (SeguridadProperties.Key key : seguridadProperties.jwt().keys()) {
            if (esRutaDeCertificadoDeDesarrollo(key.privateKeyLocation())
                    || esRutaDeCertificadoDeDesarrollo(key.publicKeyLocation())) {
                problemas.add("La llave JWT '" + key.kid() + "' apunta a un certificado de desarrollo.");
            }
        }

        if (!problemas.isEmpty()) {
            throw new IllegalStateException(
                    "Arranque en perfil 'prod' rechazado por configuración insegura: " + problemas);
        }
    }

    private boolean esRutaDeCertificadoDeDesarrollo(String location) {
        return location != null && location.contains(MARCADOR_CERTIFICADO_DEV);
    }
}
