package com.ais.marketbackend.inventario.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.inventario.api.mappers.InventarioApiMapper;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
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

class InventarioControllerTest {

    private InventarioService inventarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        inventarioService = mock(InventarioService.class);
        InventarioController controller = new InventarioController(inventarioService, new InventarioApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveInventarioDeLaTiendaPaginado() throws Exception {
        when(inventarioService.listarPorTienda(1L, 0, 20)).thenReturn(new Pagina<>(
                List.of(new InventarioResumen(1L, 1L, 2L, new BigDecimal("10.000"), new BigDecimal("5.0000"))),
                0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/inventario/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].existenciaActual").value("10.000"))
                .andExpect(jsonPath("$.contenido[0].costoPromedioActual").value("5.0000"));
    }

    @Test
    void listarMovimientosDevuelveKardexPaginado() throws Exception {
        when(inventarioService.listarMovimientos(1L, 2L, 0, 20)).thenReturn(new Pagina<>(List.of(
                new MovimientoInventarioResumen(1L, Instant.parse("2026-01-01T00:00:00Z"), 1L, 2L,
                        new BigDecimal("10.000"), new BigDecimal("5.0000"), TipoMovimiento.COMPRA)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/inventario/tiendas/1/productos/2/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipoMovimiento").value("COMPRA"));
    }

    @Test
    void registrarMovimientoDevuelve201() throws Exception {
        when(inventarioService.registrarMovimiento(1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA))
                .thenReturn(new InventarioResumen(1L, 1L, 2L, new BigDecimal("10.000"), new BigDecimal("5.0000")));

        mockMvc.perform(post("/api/v1/inventario/tiendas/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":2,\"cantidad\":10,\"costoUnitario\":5.00,\"tipoMovimiento\":\"COMPRA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.existenciaActual").value("10.000"));
    }

    @Test
    void registrarMovimientoNoPermitidoDevuelve400() throws Exception {
        when(inventarioService.registrarMovimiento(any(), any(), any(), any(), any()))
                .thenThrow(new MovimientoNoPermitidoException("no permitido"));

        mockMvc.perform(post("/api/v1/inventario/tiendas/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":2,\"cantidad\":10,\"costoUnitario\":5.00,\"tipoMovimiento\":\"COMPRA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOVIMIENTO_NO_PERMITIDO"));
    }

    @Test
    void registrarMovimientoConCantidadNegativaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/inventario/tiendas/1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":2,\"cantidad\":-1,\"costoUnitario\":5.00,\"tipoMovimiento\":\"COMPRA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
