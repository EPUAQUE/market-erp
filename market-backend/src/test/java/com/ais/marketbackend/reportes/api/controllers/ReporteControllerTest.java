package com.ais.marketbackend.reportes.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.reportes.api.mappers.ReporteApiMapper;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteCompra;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteVenta;
import com.ais.marketbackend.reportes.application.dtos.ReporteComprasResumen;
import com.ais.marketbackend.reportes.application.dtos.ReporteVentasResumen;
import com.ais.marketbackend.reportes.application.services.interfaces.ReporteService;
import com.ais.marketbackend.reportes.domain.exception.RangoFechasInvalidoException;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReporteControllerTest {

    private ReporteService reporteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reporteService = mock(ReporteService.class);
        ReporteController controller = new ReporteController(reporteService, new ReporteApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void reporteVentasDevuelveElResumenDelRango() throws Exception {
        when(reporteService.reporteVentas(eq(1L), any(), any())).thenReturn(new ReporteVentasResumen(
                1L, Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-08-31T23:59:59Z"),
                new BigDecimal("100.00"), 1,
                List.of(new LineaReporteVenta(9L, 2L, Instant.parse("2026-08-15T00:00:00Z"), new BigDecimal("100.00")))));

        mockMvc.perform(get("/api/v1/reportes/tiendas/1/ventas")
                        .param("desde", "2026-08-01T00:00:00Z")
                        .param("hasta", "2026-08-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").value("100.00"))
                .andExpect(jsonPath("$.cantidadVentas").value(1))
                .andExpect(jsonPath("$.lineas[0].ventaId").value(9));
    }

    @Test
    void reporteVentasConRangoInvertidoDevuelve400() throws Exception {
        when(reporteService.reporteVentas(eq(1L), any(), any())).thenThrow(new RangoFechasInvalidoException());

        mockMvc.perform(get("/api/v1/reportes/tiendas/1/ventas")
                        .param("desde", "2026-08-31T23:59:59Z")
                        .param("hasta", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RANGO_FECHAS_INVALIDO"));
    }

    @Test
    void reporteComprasDevuelveElResumenDelRango() throws Exception {
        when(reporteService.reporteCompras(eq(1L), any(), any())).thenReturn(new ReporteComprasResumen(
                1L, Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-08-31T23:59:59Z"),
                new BigDecimal("40.00"), 1,
                List.of(new LineaReporteCompra(9L, 2L, Instant.parse("2026-08-15T00:00:00Z"), new BigDecimal("40.00")))));

        mockMvc.perform(get("/api/v1/reportes/tiendas/1/compras")
                        .param("desde", "2026-08-01T00:00:00Z")
                        .param("hasta", "2026-08-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompras").value("40.00"))
                .andExpect(jsonPath("$.cantidadCompras").value(1));
    }
}
