package com.ais.marketbackend.traslados.api.controllers;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.ais.marketbackend.traslados.api.mappers.TrasladoApiMapper;
import com.ais.marketbackend.traslados.application.dtos.LineaTrasladoResumen;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import com.ais.marketbackend.traslados.application.services.interfaces.TrasladoService;
import com.ais.marketbackend.traslados.domain.exception.EstadoTrasladoInvalidoException;
import com.ais.marketbackend.traslados.domain.model.EstadoTraslado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrasladoControllerTest {

    private TrasladoService trasladoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        trasladoService = mock(TrasladoService.class);
        TrasladoController controller = new TrasladoController(trasladoService, new TrasladoApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLosTrasladosPaginados() throws Exception {
        when(trasladoService.listar(0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(5L, EstadoTraslado.BORRADOR)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/traslados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("BORRADOR"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(trasladoService.crear(eq(1L), eq(2L), anyList())).thenReturn(resumen(5L, EstadoTraslado.BORRADOR));

        mockMvc.perform(post("/api/v1/traslados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaOrigenId\":1,\"tiendaDestinoId\":2,\"lineas\":[{\"productoId\":10,\"cantidad\":5}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crearSinLineasDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/traslados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaOrigenId\":1,\"tiendaDestinoId\":2,\"lineas\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completarDevuelveElTrasladoActualizado() throws Exception {
        when(trasladoService.completar(5L)).thenReturn(resumen(5L, EstadoTraslado.COMPLETADO));

        mockMvc.perform(post("/api/v1/traslados/5/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    void completarConStockInsuficienteDevuelve409() throws Exception {
        when(trasladoService.completar(5L)).thenThrow(new StockInsuficienteException(10L, 1L));

        mockMvc.perform(post("/api/v1/traslados/5/completar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STOCK_INSUFICIENTE"));
    }

    @Test
    void anularConEstadoInvalidoDevuelve400() throws Exception {
        when(trasladoService.anular(5L)).thenThrow(new EstadoTrasladoInvalidoException(EstadoTraslado.COMPLETADO));

        mockMvc.perform(post("/api/v1/traslados/5/anular"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ESTADO_TRASLADO_INVALIDO"));
    }

    private TrasladoResumen resumen(Long id, EstadoTraslado estado) {
        List<LineaTrasladoResumen> lineas = List.of(new LineaTrasladoResumen(1L, 10L, new BigDecimal("5")));
        return new TrasladoResumen(id, 1L, 2L, Instant.parse("2026-01-01T00:00:00Z"), estado, lineas);
    }
}
