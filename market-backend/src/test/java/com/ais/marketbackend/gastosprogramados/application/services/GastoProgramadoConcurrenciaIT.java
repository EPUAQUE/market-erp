package com.ais.marketbackend.gastosprogramados.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.gastosprogramados.domain.exception.GastoNoVencidoException;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 3 del plan (PLAN_MEJORAS.md): antes,
 * {@code GastoProgramadoServiceImpl.generarPago} leía el gasto con
 * {@code findById} (sin bloqueo) — dos ejecuciones casi simultáneas del mismo
 * gasto podían leer la misma {@code proximaFecha} vencida y ambas pasar la
 * validación, generando dos pagos para el mismo período (con riesgo además de
 * perder uno de los dos por la colección JPA con {@code orphanRemoval}, igual
 * que Caja/CxC/CxP). Ahora usa {@code findByIdConBloqueo} (`PESSIMISTIC_WRITE`).
 *
 * <p>Esta prueba corre dos {@code generarPago} realmente concurrentes contra
 * Postgres real (no mocks) para un gasto con exactamente un período vencido:
 * exactamente uno tiene éxito, el otro ve el período ya cubierto por el
 * primero y recibe {@code GastoNoVencidoException} — nunca dos pagos para el
 * mismo período.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class GastoProgramadoConcurrenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GrupoTiendaService grupoTiendaService;
    @Autowired
    private TiendaService tiendaService;
    @Autowired
    private GastoProgramadoService gastoProgramadoService;

    private Long tiendaId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-GASTO", "Grupo de prueba gasto");
        TiendaResumen tienda =
                tiendaService.crear("T-GASTO", "Tienda de prueba gasto", null, null, null, grupo.id());
        tiendaId = tienda.id();
    }

    @Test
    void dosEjecucionesConcurrentesDelMismoPeriodoProducenUnSoloPago() throws Exception {
        GastoProgramadoResumen gasto = gastoProgramadoService.crear(
                tiendaId, "Renta local", new BigDecimal("1500.00"), FrecuenciaGasto.MENSUAL,
                Instant.now().minusSeconds(60));

        List<Callable<Boolean>> tareas = List.of(
                () -> generarPagoSiEsPosible(gasto.id()), () -> generarPagoSiEsPosible(gasto.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        GastoProgramadoResumen actual = gastoProgramadoService.obtener(tiendaId, gasto.id());
        assertThat(actual.pagos()).hasSize(1);
    }

    private boolean generarPagoSiEsPosible(Long gastoId) {
        try {
            gastoProgramadoService.generarPago(tiendaId, gastoId);
            return true;
        } catch (GastoNoVencidoException e) {
            return false;
        }
    }

    private Boolean obtenerResultado(Future<Boolean> futuro) {
        try {
            return futuro.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
