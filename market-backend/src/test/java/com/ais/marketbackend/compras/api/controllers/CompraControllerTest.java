package com.ais.marketbackend.compras.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.compras.api.mappers.CompraApiMapper;
import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.LineaCompraResumen;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.compras.domain.exception.EstadoCompraInvalidoException;
import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
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

class CompraControllerTest {

    private CompraService compraService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        compraService = mock(CompraService.class);
        CompraController controller = new CompraController(compraService, new CompraApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLasComprasDeLaTiendaPaginadas() throws Exception {
        when(compraService.listarPorTienda(1L, 0, 20))
                .thenReturn(new Pagina<>(List.of(resumen(5L, EstadoCompra.BORRADOR)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/compras/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("BORRADOR"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(compraService.crear(eq(1L), eq(2L), anyList())).thenReturn(resumen(5L, EstadoCompra.BORRADOR));

        mockMvc.perform(post("/api/v1/compras/tiendas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":2,\"lineas\":[{\"productoId\":10,\"cantidad\":10,\"costoUnitario\":5.00}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crearSinLineasDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/compras/tiendas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":2,\"lineas\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recibirDevuelveLaCompraActualizada() throws Exception {
        when(compraService.recibir(1L, 5L)).thenReturn(resumen(5L, EstadoCompra.RECIBIDA));

        mockMvc.perform(post("/api/v1/compras/tiendas/1/5/recibir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));
    }

    @Test
    void recibirConMovimientoNoPermitidoDevuelve400() throws Exception {
        when(compraService.recibir(1L, 5L)).thenThrow(new MovimientoNoPermitidoException("no permitido"));

        mockMvc.perform(post("/api/v1/compras/tiendas/1/5/recibir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOVIMIENTO_NO_PERMITIDO"));
    }

    @Test
    void anularConEstadoInvalidoDevuelve400() throws Exception {
        when(compraService.anular(1L, 5L)).thenThrow(new EstadoCompraInvalidoException(EstadoCompra.RECIBIDA));

        mockMvc.perform(post("/api/v1/compras/tiendas/1/5/anular"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ESTADO_COMPRA_INVALIDO"));
    }

    private CompraResumen resumen(Long id, EstadoCompra estado) {
        List<LineaCompraResumen> lineas = List.of(new LineaCompraResumen(1L, 10L, new BigDecimal("10"), new BigDecimal("5.00")));
        return new CompraResumen(id, 2L, 1L, Instant.parse("2026-01-01T00:00:00Z"), estado, lineas, new BigDecimal("50.00"));
    }
}
