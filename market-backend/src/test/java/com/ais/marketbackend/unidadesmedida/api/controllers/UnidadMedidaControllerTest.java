package com.ais.marketbackend.unidadesmedida.api.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.ais.marketbackend.unidadesmedida.api.dtos.responses.UnidadMedidaResponse;
import com.ais.marketbackend.unidadesmedida.api.mappers.UnidadMedidaApiMapper;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import com.ais.marketbackend.unidadesmedida.domain.exception.UnidadMedidaDuplicadaException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UnidadMedidaControllerTest {

    private UnidadMedidaService unidadMedidaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        unidadMedidaService = mock(UnidadMedidaService.class);
        UnidadMedidaApiMapper mapper = resumen -> UnidadMedidaResponse.builder()
                .id(resumen.id())
                .nombre(resumen.nombre())
                .abreviacion(resumen.abreviacion())
                .build();

        UnidadMedidaController controller = new UnidadMedidaController(unidadMedidaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLasUnidades() throws Exception {
        when(unidadMedidaService.listar()).thenReturn(List.of(new UnidadMedidaResumen(1L, "Kilogramo", "kg")));

        mockMvc.perform(get("/api/v1/unidades-medida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].abreviacion").value("kg"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(unidadMedidaService.crear(anyString(), anyString()))
                .thenReturn(new UnidadMedidaResumen(2L, "Litro", "l"));

        mockMvc.perform(post("/api/v1/unidades-medida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Litro\",\"abreviacion\":\"l\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Litro"));
    }

    @Test
    void crearDuplicadaDevuelve409() throws Exception {
        when(unidadMedidaService.crear(anyString(), anyString()))
                .thenThrow(new UnidadMedidaDuplicadaException("Kilogramo"));

        mockMvc.perform(post("/api/v1/unidades-medida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Kilogramo\",\"abreviacion\":\"kg\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UNIDAD_MEDIDA_DUPLICADA"));
    }

    @Test
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/unidades-medida")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"abreviacion\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
