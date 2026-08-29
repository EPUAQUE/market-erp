package com.ais.marketbackend.seguridad.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.seguridad.application.dtos.RolResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.RolService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.exception.AsignacionMixtaNoPermitidaException;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
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
 * Fase 3 del plan (PLAN_MEJORAS.md — hallazgo de la auditoría general, cerrado
 * 2026-08-29): {@code UsuarioServiceImpl.asignarTienda}/{@code asignarGrupo} leían
 * el usuario con {@code findById} (sin bloqueo) antes de comprobar "asignación mixta
 * no permitida" contra la OTRA tabla (`usuario_tienda`/`usuario_grupo_tienda`) — sin
 * ninguna restricción de BD que abarque ambas a la vez. Un {@code asignarTienda} y un
 * {@code asignarGrupo} concurrentes para el mismo usuario (con la tienda perteneciendo
 * al grupo) podían ambos leer la otra tabla vacía y pasar la validación, dejando al
 * usuario con una tienda individual y el grupo de esa misma tienda a la vez. Ahora
 * ambos usan {@code findByIdConBloqueo} (`PESSIMISTIC_WRITE`) sobre la fila del
 * usuario como punto de serialización compartido entre las dos tablas.
 *
 * <p>Esta prueba corre un {@code asignarTienda} y un {@code asignarGrupo} realmente
 * concurrentes contra Postgres real (no mocks) para el mismo usuario, con la tienda
 * perteneciendo al grupo: exactamente uno tiene éxito.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AsignacionMixtaConcurrenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GrupoTiendaService grupoTiendaService;
    @Autowired
    private TiendaService tiendaService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private RolService rolService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    @Test
    void asignarTiendaYAsignarGrupoConcurrentesParaElMismoUsuarioSoloUnoTieneExito() throws Exception {
        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-MIXTA", "Grupo de prueba asignación mixta");
        TiendaResumen tienda =
                tiendaService.crear("T-MIXTA", "Tienda de prueba asignación mixta", null, null, null, grupo.id());
        UsuarioResumen usuario = usuarioService.crear(
                "usuario.mixta." + System.nanoTime(), "clave-larga-segura-123", "Usuario Prueba Mixta",
                "12345678", "mixta@example.com");
        Long rolId = rolService.listar().stream()
                .filter(r -> "CAJERO".equals(r.nombre()))
                .map(RolResumen::id)
                .findFirst()
                .orElseThrow();

        List<Callable<Boolean>> tareas = List.of(
                () -> asignarTiendaSiEsPosible(usuario.id(), tienda.id(), rolId),
                () -> asignarGrupoSiEsPosible(usuario.id(), grupo.id(), rolId));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        int totalAsignaciones =
                usuarioService.listarTiendas(usuario.id()).size() + usuarioService.listarGrupos(usuario.id()).size();
        assertThat(totalAsignaciones).isEqualTo(1);
    }

    private boolean asignarTiendaSiEsPosible(Long usuarioId, Long tiendaId, Long rolId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
        try {
            usuarioService.asignarTienda(usuarioId, tiendaId, rolId);
            return true;
        } catch (AsignacionMixtaNoPermitidaException e) {
            return false;
        }
    }

    private boolean asignarGrupoSiEsPosible(Long usuarioId, Long grupoId, Long rolId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
        try {
            usuarioService.asignarGrupo(usuarioId, grupoId, rolId);
            return true;
        } catch (AsignacionMixtaNoPermitidaException e) {
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
