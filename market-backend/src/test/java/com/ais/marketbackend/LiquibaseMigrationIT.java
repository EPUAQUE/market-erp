package com.ais.marketbackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de integración real (Fase 7 del plan): levanta un Postgres efímero vía
 * Testcontainers y carga el contexto completo de Spring Boot contra él — Liquibase
 * migra desde cero y Hibernate valida el esquema resultante ({@code ddl-auto:
 * validate}) antes de que el contexto termine de arrancar. Corre solo en
 * {@code mvn verify} (plugin failsafe), nunca en {@code mvn test}, porque necesita
 * Docker disponible.
 *
 * <p>Sin esta prueba, un error de arranque solo se detecta manualmente vía Docker
 * Compose — así se encontró el bug de la Fase 4 (un método de repositorio con
 * derivación de nombre inválida que Spring Data no puede resolver en un
 * {@code @Bean}), invisible para la suite de pruebas unitarias con mocks.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class LiquibaseMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void elContextoArrancaYLiquibaseMigraUnaBaseVaciaDesdeCero() {
        Integer changesetsAplicados =
                jdbcTemplate.queryForObject("select count(*) from databasechangelog", Integer.class);

        assertThat(changesetsAplicados).isGreaterThan(0);
    }
}
