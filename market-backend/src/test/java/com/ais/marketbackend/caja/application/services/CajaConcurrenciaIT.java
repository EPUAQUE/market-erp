package com.ais.marketbackend.caja.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.exception.CajaSesionAbiertaException;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): antes, {@code CajaServiceImpl.abrir} solo
 * comprobaba en memoria si ya había una caja abierta (sin bloqueo ni restricción en
 * BD) — dos aperturas concurrentes para la misma tienda podían ambas ver "no hay
 * caja abierta" y terminar creando dos sesiones ABIERTA a la vez. Y
 * {@code registrarMovimiento}/{@code cerrar} leían la sesión sin bloqueo — dos
 * movimientos concurrentes podían perderse entre sí (colección JPA con
 * {@code orphanRemoval}, ver {@code CajaServiceImpl}).
 *
 * <p>Ahora {@code abrir} se apoya en el índice único parcial
 * {@code ux_caja_sesion_abierta_por_tienda} (traducido a
 * {@code CajaSesionAbiertaException}), y {@code registrarMovimiento}/{@code cerrar}
 * bloquean la fila con {@code PESSIMISTIC_WRITE} durante toda la operación. Esta
 * prueba corre aperturas y movimientos realmente concurrentes contra Postgres real
 * (no mocks).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CajaConcurrenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GrupoTiendaService grupoTiendaService;
    @Autowired
    private TiendaService tiendaService;
    @Autowired
    private CajaService cajaService;

    @BeforeEach
    void setUp() {
        // CajaService no exige autenticación, pero GrupoTiendaService/TiendaService sí
        // (ver VentaCreditoConcurrenciaIT) — se simula el admin sembrado por
        // AdminUserSeeder (perfil test, seed.enabled=true).
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    private Long crearTienda(String sufijo) {
        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-CAJA-" + sufijo, "Grupo de prueba caja " + sufijo);
        TiendaResumen tienda =
                tiendaService.crear("T-CAJA-" + sufijo, "Tienda de prueba caja " + sufijo, null, null, null, grupo.id());
        return tienda.id();
    }

    @Test
    void dosAperturasConcurrentesParaLaMismaTiendaSoloUnaTieneExito() throws Exception {
        Long tiendaId = crearTienda("A");

        List<Callable<Boolean>> tareas = List.of(
                () -> abrirSiEsPosible(tiendaId), () -> abrirSiEsPosible(tiendaId));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(cajaService.obtenerAbierta(tiendaId)).isNotNull();
    }

    @Test
    void diezMovimientosConcurrentesSobreLaMismaCajaProducenSaldoFinalExacto() throws Exception {
        Long tiendaId = crearTienda("B");
        cajaService.abrir(tiendaId, new BigDecimal("100.00"));

        List<Callable<CajaSesionResumen>> tareas = Stream.generate(
                        () -> (Callable<CajaSesionResumen>) () -> cajaService.registrarMovimiento(
                                tiendaId, TipoMovimientoCaja.INGRESO, "Ingreso concurrente", new BigDecimal("10.00")))
                .limit(10)
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Future<CajaSesionResumen>> futuros = executor.invokeAll(tareas);
            futuros.forEach(this::obtenerResultado);
        } finally {
            executor.shutdown();
        }

        CajaSesionResumen sesion = cajaService.obtenerAbierta(tiendaId);
        assertThat(sesion.movimientos()).hasSize(10);
        assertThat(sesion.saldoEsperado()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    private boolean abrirSiEsPosible(Long tiendaId) {
        try {
            cajaService.abrir(tiendaId, new BigDecimal("100.00"));
            return true;
        } catch (CajaSesionAbiertaException e) {
            return false;
        }
    }

    private <T> T obtenerResultado(Future<T> futuro) {
        try {
            return futuro.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
