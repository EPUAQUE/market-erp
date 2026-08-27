package com.ais.marketbackend.grupostienda.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.grupostienda.api.mappers.GrupoTiendaApiMapper;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.grupostienda.domain.exception.GrupoTiendaDuplicadoException;
import com.ais.marketbackend.grupostienda.domain.model.EstadoGrupoTienda;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GrupoTiendaControllerTest {

    private GrupoTiendaService grupoTiendaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        grupoTiendaService = mock(GrupoTiendaService.class);
        GrupoTiendaApiMapper mapper = resumen ->
                com.ais.marketbackend.grupostienda.api.dtos.responses.GrupoTiendaResponse.builder()
                        .id(resumen.id())
                        .codigo(resumen.codigo())
                        .nombre(resumen.nombre())
                        .estado(resumen.estado())
                        .build();

        GrupoTiendaController controller = new GrupoTiendaController(grupoTiendaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLosGrupos() throws Exception {
        when(grupoTiendaService.listar()).thenReturn(List.of(
                new GrupoTiendaResumen(1L, "PRINCIPAL", "Grupo Principal", EstadoGrupoTienda.ACTIVO)));

        mockMvc.perform(get("/api/v1/grupos-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PRINCIPAL"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(grupoTiendaService.crear(anyString(), anyString()))
                .thenReturn(new GrupoTiendaResumen(2L, "NORTE", "Grupo Norte", EstadoGrupoTienda.ACTIVO));

        mockMvc.perform(post("/api/v1/grupos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"norte\",\"nombre\":\"Grupo Norte\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("NORTE"));
    }

    @Test
    void crearConCodigoDuplicadoDevuelve409() throws Exception {
        when(grupoTiendaService.crear(anyString(), anyString()))
                .thenThrow(new GrupoTiendaDuplicadoException("PRINCIPAL"));

        mockMvc.perform(post("/api/v1/grupos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"principal\",\"nombre\":\"Otro\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("GRUPO_TIENDA_DUPLICADO"));
    }

    @Test
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/grupos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarDelegaAlServicio() throws Exception {
        when(grupoTiendaService.actualizar(1L, "Nuevo nombre"))
                .thenReturn(new GrupoTiendaResumen(1L, "PRINCIPAL", "Nuevo nombre", EstadoGrupoTienda.ACTIVO));

        mockMvc.perform(put("/api/v1/grupos-tienda/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
    }

    @Test
    void activarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/grupos-tienda/1/activar"))
                .andExpect(status().isNoContent());

        verify(grupoTiendaService).activar(1L);
    }

    @Test
    void desactivarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/grupos-tienda/1/desactivar"))
                .andExpect(status().isNoContent());

        verify(grupoTiendaService).desactivar(1L);
    }
}
