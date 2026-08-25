package com.ais.marketbackend.categorias.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.categorias.api.dtos.responses.CategoriaResponse;
import com.ais.marketbackend.categorias.api.mappers.CategoriaApiMapper;
import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.categorias.domain.model.EstadoCategoria;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoriaControllerTest {

    private CategoriaService categoriaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoriaService = mock(CategoriaService.class);
        CategoriaApiMapper mapper = resumen -> CategoriaResponse.builder()
                .id(resumen.id())
                .nombre(resumen.nombre())
                .imagen(resumen.imagen())
                .estado(resumen.estado())
                .build();

        CategoriaController controller = new CategoriaController(categoriaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLasCategorias() throws Exception {
        when(categoriaService.listar()).thenReturn(
                List.of(new CategoriaResumen(1L, "Bebidas", null, EstadoCategoria.ACTIVA)));

        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Bebidas"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(categoriaService.crear(anyString(), any()))
                .thenReturn(new CategoriaResumen(2L, "Lácteos", null, EstadoCategoria.ACTIVA));

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Lácteos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Lácteos"));
    }

    @Test
    void crearConJsonMalformadoDevuelve400NoQuinientos() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void activarYDesactivarDevuelven204() throws Exception {
        mockMvc.perform(post("/api/v1/categorias/1/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/categorias/1/activar")).andExpect(status().isNoContent());

        verify(categoriaService).desactivar(1L);
        verify(categoriaService).activar(1L);
    }
}
