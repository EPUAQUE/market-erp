package com.ais.marketbackend.fel.infrastructure.persistence.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.fel.domain.repository.DocumentoFelRepository;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 1 del plan (PLAN_MEJORAS.md): antes, el correlativo FEL se calculaba con
 * {@code MAX(numero) + 1} sin bloqueo — dos emisiones concurrentes para la misma
 * (tienda, serie) podían leer el mismo máximo y terminar con el mismo número. Esta
 * prueba corre dos llamadas realmente concurrentes contra Postgres real (no mocks)
 * y verifica que {@link DocumentoFelRepository#siguienteNumero} nunca repite un
 * número, incluida la primera vez que se usa una serie nueva (colisión de creación
 * concurrente de la fila de {@code fel_correlativo}, ver
 * {@code DocumentoFelRepositoryAdapter}).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FelCorrelativoConcurrenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Long TIENDA_ID = 1L;

    @Autowired
    private DocumentoFelRepository documentoFelRepository;

    @Test
    void dosEmisionesConcurrentesSobreSerieNuevaRecibenCorrelativosDistintos() throws Exception {
        String serie = serieDePrueba("A");
        List<Long> numeros = ejecutarEnParaleloYEsperar(
                () -> documentoFelRepository.siguienteNumero(TIENDA_ID, serie),
                () -> documentoFelRepository.siguienteNumero(TIENDA_ID, serie));

        assertThat(numeros).doesNotHaveDuplicates();
    }

    @Test
    void diezEmisionesConcurrentesSobreLaMismaSerieRecibenDiezCorrelativosDistintos() throws Exception {
        String serie = serieDePrueba("B");
        List<Callable<Long>> tareas = Stream.generate(
                        () -> (Callable<Long>) () -> documentoFelRepository.siguienteNumero(TIENDA_ID, serie))
                .limit(10)
                .toList();

        List<Long> numeros = ejecutarEnParaleloYEsperar(tareas);

        assertThat(numeros).doesNotHaveDuplicates();
        assertThat(numeros).hasSize(10);
    }

    /** {@code serie} es {@code VARCHAR(10)} en BD — prefijo de 1 char + 8 dígitos, cabe con margen. */
    private String serieDePrueba(String prefijo) {
        return prefijo + (System.nanoTime() % 100_000_000L);
    }

    @SafeVarargs
    private List<Long> ejecutarEnParaleloYEsperar(Callable<Long>... tareas) throws Exception {
        return ejecutarEnParaleloYEsperar(List.of(tareas));
    }

    private List<Long> ejecutarEnParaleloYEsperar(List<Callable<Long>> tareas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tareas.size());
        try {
            List<Future<Long>> futuros = executor.invokeAll(tareas);
            return futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }
    }

    private Long obtenerResultado(Future<Long> futuro) {
        try {
            return futuro.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
