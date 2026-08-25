package com.ais.marketbackend.cuentasporcobrar.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.cuentasporcobrar.api.mappers.CuentaPorCobrarApiMapper;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CobroExcedeSaldoException;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
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

class CuentaPorCobrarControllerTest {

    private CuentaPorCobrarService cuentaPorCobrarService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cuentaPorCobrarService = mock(CuentaPorCobrarService.class);
        CuentaPorCobrarController controller =
                new CuentaPorCobrarController(cuentaPorCobrarService, new CuentaPorCobrarApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLasCuentasDeLaTiendaPaginadas() throws Exception {
        when(cuentaPorCobrarService.listarPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(9L, EstadoCuentaPorCobrar.PENDIENTE)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/cuentas-por-cobrar/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"));
    }

    @Test
    void registrarCobroDevuelveLaCuentaActualizada() throws Exception {
        when(cuentaPorCobrarService.registrarCobro(1L, 9L, new BigDecimal("30.00"), MetodoPago.EFECTIVO))
                .thenReturn(resumen(9L, EstadoCuentaPorCobrar.PENDIENTE));

        mockMvc.perform(post("/api/v1/cuentas-por-cobrar/tiendas/1/9/cobros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":30.00,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarCobroQueExcedeElSaldoDevuelve400() throws Exception {
        when(cuentaPorCobrarService.registrarCobro(1L, 9L, new BigDecimal("999.00"), MetodoPago.EFECTIVO))
                .thenThrow(new CobroExcedeSaldoException());

        mockMvc.perform(post("/api/v1/cuentas-por-cobrar/tiendas/1/9/cobros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":999.00,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COBRO_EXCEDE_SALDO"));
    }

    @Test
    void registrarCobroConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/cuentas-por-cobrar/tiendas/1/9/cobros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":-5,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anularDevuelveLaCuentaAnulada() throws Exception {
        when(cuentaPorCobrarService.anular(1L, 9L)).thenReturn(resumen(9L, EstadoCuentaPorCobrar.ANULADA));

        mockMvc.perform(post("/api/v1/cuentas-por-cobrar/tiendas/1/9/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    private CuentaPorCobrarResumen resumen(Long id, EstadoCuentaPorCobrar estado) {
        return new CuentaPorCobrarResumen(
                id, 5L, 2L, 1L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), estado, List.of());
    }
}
