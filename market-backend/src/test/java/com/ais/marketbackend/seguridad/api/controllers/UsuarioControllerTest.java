package com.ais.marketbackend.seguridad.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.seguridad.api.mappers.UsuarioApiMapper;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.model.EstadoUsuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UsuarioControllerTest {

    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        UsuarioApiMapper mapper = new UsuarioApiMapperImplForTest();
        UsuarioController controller = new UsuarioController(usuarioService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarDevuelveLosUsuariosDelServicio() throws Exception {
        when(usuarioService.listar()).thenReturn(
                List.of(new UsuarioResumen(1L, "ana", EstadoUsuario.ACTIVO, "Ana Pérez", "12345678", "ana@example.com")));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana"));
    }

    @Test
    void crearDevuelve201ConElUsuarioCreado() throws Exception {
        when(usuarioService.crear(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new UsuarioResumen(
                        2L, "beto", EstadoUsuario.ACTIVO, "Beto Gómez", "87654321", "beto@example.com"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"beto\",\"password\":\"clave-larga-segura\","
                                + "\"nombre\":\"Beto Gómez\",\"telefono\":\"87654321\","
                                + "\"correo\":\"beto@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("beto"));
    }

    @Test
    void listarTiendasDevuelveLasAsignacionesDelUsuario() throws Exception {
        when(usuarioService.listarTiendas(1L)).thenReturn(List.of(
                new com.ais.marketbackend.seguridad.application.dtos.UsuarioTiendaResumen(1L, 10L, 5L, "CAJERO")));

        mockMvc.perform(get("/api/v1/usuarios/1/tiendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tiendaId").value(10))
                .andExpect(jsonPath("$[0].rolNombre").value("CAJERO"));
    }

    @Test
    void asignarTiendaDelegaAlServicioYDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaId\":5,\"rolId\":2}"))
                .andExpect(status().isCreated());

        verify(usuarioService).asignarTienda(1L, 5L, 2L);
    }

    @Test
    void listarGruposDevuelveLasAsignacionesDelUsuario() throws Exception {
        when(usuarioService.listarGrupos(1L)).thenReturn(List.of(
                new com.ais.marketbackend.seguridad.application.dtos.UsuarioGrupoTiendaResumen(
                        1L, 5L, 9L, "ADMIN_GRUPO")));

        mockMvc.perform(get("/api/v1/usuarios/1/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupoTiendaId").value(5))
                .andExpect(jsonPath("$[0].rolNombre").value("ADMIN_GRUPO"));
    }

    @Test
    void asignarGrupoDelegaAlServicioYDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/1/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoTiendaId\":5,\"rolId\":9}"))
                .andExpect(status().isCreated());

        verify(usuarioService).asignarGrupo(1L, 5L, 9L);
    }

    @Test
    void restablecerPasswordDevuelveLaTemporalGeneradaPorElServicio() throws Exception {
        when(usuarioService.restablecerPassword(1L)).thenReturn("Ab3dEfGhJk4mNpQrSt5u");

        mockMvc.perform(post("/api/v1/usuarios/1/password/restablecer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordTemporal").value("Ab3dEfGhJk4mNpQrSt5u"));
    }

    @Test
    void revocarSesionesDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/1/sesiones/revocar"))
                .andExpect(status().isNoContent());

        verify(usuarioService).revocarSesiones(1L);
    }

    @Test
    void desactivarDelegaAlServicioYDevuelveElUsuarioActualizado() throws Exception {
        when(usuarioService.desactivar(1L)).thenReturn(
                new UsuarioResumen(1L, "ana", EstadoUsuario.INACTIVO, "Ana Pérez", "12345678", "ana@example.com"));

        mockMvc.perform(post("/api/v1/usuarios/1/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        verify(usuarioService).desactivar(1L);
    }

    @Test
    void bloquearDelegaAlServicioYDevuelveElUsuarioActualizado() throws Exception {
        when(usuarioService.bloquear(1L)).thenReturn(
                new UsuarioResumen(1L, "ana", EstadoUsuario.BLOQUEADO, "Ana Pérez", "12345678", "ana@example.com"));

        mockMvc.perform(post("/api/v1/usuarios/1/bloquear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));

        verify(usuarioService).bloquear(1L);
    }

    @Test
    void activarDelegaAlServicioYDevuelveElUsuarioActualizado() throws Exception {
        when(usuarioService.activar(1L)).thenReturn(
                new UsuarioResumen(1L, "ana", EstadoUsuario.ACTIVO, "Ana Pérez", "12345678", "ana@example.com"));

        mockMvc.perform(post("/api/v1/usuarios/1/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        verify(usuarioService).activar(1L);
    }

    /** Mapper manual mínimo para el test — evita depender del bean generado por MapStruct en este módulo aislado. */
    private static class UsuarioApiMapperImplForTest implements UsuarioApiMapper {
        @Override
        public com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse toResponse(UsuarioResumen resumen) {
            return com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse.builder()
                    .id(resumen.id())
                    .username(resumen.username())
                    .estado(resumen.estado())
                    .nombre(resumen.nombre())
                    .telefono(resumen.telefono())
                    .correo(resumen.correo())
                    .build();
        }
    }
}
