package com.ais.marketbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 1 del plan (docs/plan-mejoras.md): pruebas de arranque por perfil — antes
 * bloqueadas porque no existían {@code application-{local,test,prod}.yml}. Cada test
 * arranca su propio contexto con {@link SpringApplicationBuilder} (no
 * {@code @SpringBootTest}) para poder variar el perfil y las propiedades por método
 * sin interferencia entre casos; el Postgres de Testcontainers se comparte para no
 * pagar el arranque del contenedor 4 veces.
 */
@Testcontainers
class ProfileStartupIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private ConfigurableApplicationContext contexto;

    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
    }

    @Test
    void perfilLocalArrancaYSiembraElAdminPorDefecto() {
        contexto = builder("local").properties(propiedadesDeBaseDeDatos()).run();

        JdbcTemplate jdbcTemplate = contexto.getBean(JdbcTemplate.class);
        Integer admins = jdbcTemplate.queryForObject(
                "select count(*) from usuario where username = 'admin'", Integer.class);

        assertThat(admins).isEqualTo(1);
    }

    @Test
    void perfilProdSinJwtIssuerNoArranca() {
        SpringApplicationBuilder builder = builder("prod")
                .properties(propiedadesDeBaseDeDatos())
                .properties(
                        // JWT_ISSUER deliberadamente omitido: application-prod.yml lo exige
                        // sin default (${JWT_ISSUER}) — debe fallar la resolución del placeholder.
                        "JWT_AUDIENCE=market-clients-it",
                        "JWT_PRIVATE_KEY_LOCATION=classpath:certs/test-private.pem",
                        "JWT_PUBLIC_KEY_LOCATION=classpath:certs/test-public.pem",
                        "CORS_ALLOWED_ORIGINS=https://ejemplo.test",
                        "SEED_ENABLED=false");

        assertThatThrownBy(builder::run).isInstanceOf(Exception.class);
    }

    @Test
    void perfilProdConSeedHabilitadoNoArranca() {
        SpringApplicationBuilder builder = builder("prod")
                .properties(propiedadesDeBaseDeDatos())
                .properties(propiedadesJwtValidas())
                .properties("SEED_ENABLED=true");

        assertThatThrownBy(builder::run).hasStackTraceContaining("rechazado por configuración insegura");
    }

    @Test
    void perfilProdConCertificadoDeDesarrolloNoArranca() {
        SpringApplicationBuilder builder = builder("prod")
                .properties(propiedadesDeBaseDeDatos())
                .properties(
                        "JWT_ISSUER=market-backend-it",
                        "JWT_AUDIENCE=market-clients-it",
                        // Certificado marcado como "dev-*" — exactamente lo que ProdSafetyGuard rechaza.
                        "JWT_PRIVATE_KEY_LOCATION=file:./local-dev/certs/dev-private.pem",
                        "JWT_PUBLIC_KEY_LOCATION=file:./local-dev/certs/dev-public.pem",
                        "CORS_ALLOWED_ORIGINS=https://ejemplo.test",
                        "SEED_ENABLED=false");

        assertThatThrownBy(builder::run).hasStackTraceContaining("rechazado por configuración insegura");
    }

    @Test
    void perfilProdSinCertificadorFelRealNoArranca() {
        // Configuración por lo demás válida: DevCertificadorFelAdapter (el único
        // CertificadorFelPort del código) está restringido a @Profile("!prod"), así
        // que en 'prod' no hay ningún bean que satisfaga el puerto.
        SpringApplicationBuilder builder = builder("prod")
                .properties(propiedadesDeBaseDeDatos())
                .properties(propiedadesJwtValidas())
                .properties("SEED_ENABLED=false");

        assertThatThrownBy(builder::run)
                .hasStackTraceContaining("rechazado por configuración insegura")
                .hasStackTraceContaining("CertificadorFelPort");
    }

    private SpringApplicationBuilder builder(String perfil) {
        // SERVLET, no NONE: RequiresPermissionStartupValidator depende de que exista
        // infraestructura de Spring MVC (RequestMappingHandlerMapping) — con NONE ese
        // bean no existe y el arranque falla por una razón ajena a lo que prueba esta
        // clase. server.port=0 evita choques de puerto entre los tests de esta clase.
        return new SpringApplicationBuilder(MarketBackendApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles(perfil)
                .properties("server.port=0");
    }

    private String[] propiedadesDeBaseDeDatos() {
        return new String[] {
            "DB_URL=" + postgres.getJdbcUrl(),
            "DB_USERNAME=" + postgres.getUsername(),
            "DB_PASSWORD=" + postgres.getPassword()
        };
    }

    private String[] propiedadesJwtValidas() {
        return new String[] {
            "JWT_ISSUER=market-backend-it",
            "JWT_AUDIENCE=market-clients-it",
            "JWT_PRIVATE_KEY_LOCATION=classpath:certs/test-private.pem",
            "JWT_PUBLIC_KEY_LOCATION=classpath:certs/test-public.pem",
            "CORS_ALLOWED_ORIGINS=https://ejemplo.test"
        };
    }
}
