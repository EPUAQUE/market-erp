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
        when(usuarioService.listar()).thenReturn(List.of(new UsuarioResumen(1L, "ana", EstadoUsuario.ACTIVO)));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana"));
    }

    @Test
    void crearDevuelve201ConElUsuarioCreado() throws Exception {
        when(usuarioService.crear(anyString(), anyString()))
                .thenReturn(new UsuarioResumen(2L, "beto", EstadoUsuario.ACTIVO));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"beto\",\"password\":\"clave-larga-segura\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("beto"));
    }

    @Test
    void asignarTiendaDelegaAlServicioYDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaId\":5,\"rolId\":2}"))
                .andExpect(status().isCreated());

        verify(usuarioService).asignarTienda(1L, 5L, 2L);
    }

    /** Mapper manual mínimo para el test — evita depender del bean generado por MapStruct en este módulo aislado. */
    private static class UsuarioApiMapperImplForTest implements UsuarioApiMapper {
        @Override
        public com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse toResponse(UsuarioResumen resumen) {
            return com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse.builder()
                    .id(resumen.id())
                    .username(resumen.username())
                    .estado(resumen.estado())
                    .build();
        }
    }
}
