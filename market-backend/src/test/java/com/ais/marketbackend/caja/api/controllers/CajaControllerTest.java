package com.ais.marketbackend.caja.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.caja.api.mappers.CajaApiMapper;
import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.exception.CajaSesionAbiertaException;
import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CajaControllerTest {

    private CajaService cajaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cajaService = mock(CajaService.class);
        CajaController controller = new CajaController(cajaService, new CajaApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLasSesionesDeLaTiendaPaginadas() throws Exception {
        when(cajaService.listarPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(9L, EstadoCajaSesion.ABIERTA)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/caja/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("ABIERTA"));
    }

    @Test
    void abrirDevuelve201() throws Exception {
        when(cajaService.abrir(1L, new BigDecimal("100.00"), null)).thenReturn(resumen(9L, EstadoCajaSesion.ABIERTA));

        mockMvc.perform(post("/api/v1/caja/tiendas/1/abrir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
    }

    @Test
    void abrirConSesionYaAbiertaDevuelve409() throws Exception {
        when(cajaService.abrir(1L, new BigDecimal("100.00"), null)).thenThrow(new CajaSesionAbiertaException(1L));

        mockMvc.perform(post("/api/v1/caja/tiendas/1/abrir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoInicial\":100.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CAJA_SESION_ABIERTA"));
    }

    @Test
    void registrarMovimientoConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/caja/tiendas/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INGRESO\",\"concepto\":\"x\",\"monto\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cerrarDevuelveLaSesionCerrada() throws Exception {
        when(cajaService.cerrar(1L, new BigDecimal("95.00"), null)).thenReturn(resumen(9L, EstadoCajaSesion.CERRADA));

        mockMvc.perform(post("/api/v1/caja/tiendas/1/cerrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoFinalContado\":95.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
    }

    private CajaSesionResumen resumen(Long id, EstadoCajaSesion estado) {
        return new CajaSesionResumen(
                id, 1L, Instant.parse("2026-01-01T00:00:00Z"), null, new BigDecimal("100.00"), null,
                new BigDecimal("100.00"), estado, List.of());
    }
}
