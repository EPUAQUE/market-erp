package com.ais.marketbackend.marcas.api.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.marcas.api.dtos.responses.MarcaResponse;
import com.ais.marketbackend.marcas.api.mappers.MarcaApiMapper;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.marcas.domain.exception.MarcaDuplicadaException;
import com.ais.marketbackend.marcas.domain.model.EstadoMarca;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarcaControllerTest {

    private MarcaService marcaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        marcaService = mock(MarcaService.class);
        MarcaApiMapper mapper = resumen -> MarcaResponse.builder()
                .id(resumen.id())
                .nombre(resumen.nombre())
                .estado(resumen.estado())
                .build();

        MarcaController controller = new MarcaController(marcaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLasMarcas() throws Exception {
        when(marcaService.listar()).thenReturn(List.of(new MarcaResumen(1L, "Nestlé", EstadoMarca.ACTIVA)));

        mockMvc.perform(get("/api/v1/marcas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Nestlé"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(marcaService.crear(anyString())).thenReturn(new MarcaResumen(2L, "Coca-Cola", EstadoMarca.ACTIVA));

        mockMvc.perform(post("/api/v1/marcas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Coca-Cola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Coca-Cola"));
    }

    @Test
    void crearDuplicadaDevuelve409() throws Exception {
        when(marcaService.crear(anyString())).thenThrow(new MarcaDuplicadaException("Nestlé"));

        mockMvc.perform(post("/api/v1/marcas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nestlé\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MARCA_DUPLICADA"));
    }

    @Test
    void activarYDesactivarDevuelven204() throws Exception {
        mockMvc.perform(post("/api/v1/marcas/1/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/marcas/1/activar")).andExpect(status().isNoContent());

        verify(marcaService).desactivar(1L);
        verify(marcaService).activar(1L);
    }
}
