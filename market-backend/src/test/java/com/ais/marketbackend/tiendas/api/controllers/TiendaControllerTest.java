package com.ais.marketbackend.tiendas.api.controllers;

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

import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.ais.marketbackend.tiendas.api.mappers.TiendaApiMapper;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.tiendas.domain.exception.TiendaDuplicadaException;
import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TiendaControllerTest {

    private TiendaService tiendaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tiendaService = mock(TiendaService.class);
        TiendaApiMapper mapper = resumen -> com.ais.marketbackend.tiendas.api.dtos.responses.TiendaResponse.builder()
                .id(resumen.id())
                .codigo(resumen.codigo())
                .nombre(resumen.nombre())
                .direccion(resumen.direccion())
                .telefono(resumen.telefono())
                .correo(resumen.correo())
                .estado(resumen.estado())
                .grupoId(resumen.grupoId())
                .build();

        TiendaController controller = new TiendaController(tiendaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLasTiendas() throws Exception {
        when(tiendaService.listar()).thenReturn(List.of(
                new TiendaResumen(
                        1L, "CENTRAL", "Tienda Central", "Zona 1", "1234-5678", "c@x.com", EstadoTienda.ACTIVA, 1L)));

        mockMvc.perform(get("/api/v1/tiendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CENTRAL"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(tiendaService.crear(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(new TiendaResumen(2L, "NORTE", "Tienda Norte", null, null, null, EstadoTienda.ACTIVA, 1L));

        mockMvc.perform(post("/api/v1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"norte\",\"nombre\":\"Tienda Norte\",\"grupoId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("NORTE"));
    }

    @Test
    void crearConCodigoDuplicadoDevuelve409() throws Exception {
        when(tiendaService.crear(anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new TiendaDuplicadaException("CENTRAL"));

        mockMvc.perform(post("/api/v1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"central\",\"nombre\":\"Otra\",\"grupoId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TIENDA_DUPLICADA"));
    }

    @Test
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinGrupoIdDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"norte\",\"nombre\":\"Tienda Norte\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarDelegaAlServicio() throws Exception {
        when(tiendaService.actualizar(1L, "Nuevo nombre", null, null, null, 1L))
                .thenReturn(new TiendaResumen(1L, "CENTRAL", "Nuevo nombre", null, null, null, EstadoTienda.ACTIVA, 1L));

        mockMvc.perform(put("/api/v1/tiendas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo nombre\",\"grupoId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
    }

    @Test
    void activarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/tiendas/1/activar"))
                .andExpect(status().isNoContent());

        verify(tiendaService).activar(1L);
    }

    @Test
    void desactivarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/tiendas/1/desactivar"))
                .andExpect(status().isNoContent());

        verify(tiendaService).desactivar(1L);
    }
}
