package com.ais.marketbackend.cuentasporpagar.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.cuentasporpagar.api.mappers.CuentaPorPagarApiMapper;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.exception.PagoExcedeSaldoException;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
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

class CuentaPorPagarControllerTest {

    private CuentaPorPagarService cuentaPorPagarService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cuentaPorPagarService = mock(CuentaPorPagarService.class);
        CuentaPorPagarController controller =
                new CuentaPorPagarController(cuentaPorPagarService, new CuentaPorPagarApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLasCuentasDeLaTienda() throws Exception {
        when(cuentaPorPagarService.listarPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(9L, EstadoCuentaPorPagar.PENDIENTE)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/cuentas-por-pagar/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE"));
    }

    @Test
    void registrarPagoDevuelveLaCuentaActualizada() throws Exception {
        when(cuentaPorPagarService.registrarPago(1L, 9L, new BigDecimal("30.00")))
                .thenReturn(resumen(9L, EstadoCuentaPorPagar.PENDIENTE));

        mockMvc.perform(post("/api/v1/cuentas-por-pagar/tiendas/1/9/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":30.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarPagoQueExcedeElSaldoDevuelve400() throws Exception {
        when(cuentaPorPagarService.registrarPago(1L, 9L, new BigDecimal("999.00")))
                .thenThrow(new PagoExcedeSaldoException());

        mockMvc.perform(post("/api/v1/cuentas-por-pagar/tiendas/1/9/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":999.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PAGO_EXCEDE_SALDO"));
    }

    @Test
    void registrarPagoConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/cuentas-por-pagar/tiendas/1/9/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anularDevuelveLaCuentaAnulada() throws Exception {
        when(cuentaPorPagarService.anular(1L, 9L)).thenReturn(resumen(9L, EstadoCuentaPorPagar.ANULADA));

        mockMvc.perform(post("/api/v1/cuentas-por-pagar/tiendas/1/9/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    private CuentaPorPagarResumen resumen(Long id, EstadoCuentaPorPagar estado) {
        return new CuentaPorPagarResumen(
                id, 5L, 2L, 1L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), estado, List.of());
    }
}
