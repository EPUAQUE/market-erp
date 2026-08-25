package com.ais.marketbackend.gastosprogramados.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.gastosprogramados.api.mappers.GastoProgramadoApiMapper;
import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.gastosprogramados.domain.exception.GastoNoVencidoException;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
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

class GastoProgramadoControllerTest {

    private GastoProgramadoService gastoProgramadoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gastoProgramadoService = mock(GastoProgramadoService.class);
        GastoProgramadoController controller =
                new GastoProgramadoController(gastoProgramadoService, new GastoProgramadoApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLosGastosDeLaTienda() throws Exception {
        when(gastoProgramadoService.listarPorTienda(1L)).thenReturn(List.of(resumen(9L, true)));

        mockMvc.perform(get("/api/v1/gastos-programados/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void crearDevuelveElGastoCreado() throws Exception {
        when(gastoProgramadoService.crear(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2026-01-01T00:00:00Z")))
                .thenReturn(resumen(9L, true));

        mockMvc.perform(post("/api/v1/gastos-programados/tiendas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Renta local\",\"monto\":1500.00,"
                                + "\"frecuencia\":\"MENSUAL\",\"fechaInicio\":\"2026-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepto").value("Renta local"));
    }

    @Test
    void crearConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/gastos-programados/tiendas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Renta local\",\"monto\":-5,"
                                + "\"frecuencia\":\"MENSUAL\",\"fechaInicio\":\"2026-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarDevuelveElGastoActualizado() throws Exception {
        when(gastoProgramadoService.actualizar(1L, 9L, "Renta bodega", new BigDecimal("1800.00"),
                FrecuenciaGasto.QUINCENAL)).thenReturn(resumen(9L, true));

        mockMvc.perform(put("/api/v1/gastos-programados/tiendas/1/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Renta bodega\",\"monto\":1800.00,\"frecuencia\":\"QUINCENAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void generarPagoAntesDeVencerDevuelve400() throws Exception {
        when(gastoProgramadoService.generarPago(1L, 9L)).thenThrow(new GastoNoVencidoException());

        mockMvc.perform(post("/api/v1/gastos-programados/tiendas/1/9/generar-pago"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GASTO_NO_VENCIDO"));
    }

    @Test
    void desactivarDevuelveElGastoInactivo() throws Exception {
        when(gastoProgramadoService.desactivar(1L, 9L)).thenReturn(resumen(9L, false));

        mockMvc.perform(post("/api/v1/gastos-programados/tiendas/1/9/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    private GastoProgramadoResumen resumen(Long id, boolean activo) {
        return new GastoProgramadoResumen(
                id, 1L, "Renta local", new BigDecimal("1500.00"), FrecuenciaGasto.MENSUAL,
                Instant.parse("2026-01-01T00:00:00Z"), activo, List.of());
    }
}
