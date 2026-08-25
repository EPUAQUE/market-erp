package com.ais.marketbackend.productos.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.productos.api.mappers.ProductoTiendaApiMapper;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.productos.domain.exception.ConfiguracionTiendaDuplicadaException;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductoTiendaControllerTest {

    private ProductoTiendaService productoTiendaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productoTiendaService = mock(ProductoTiendaService.class);
        ProductoTiendaApiMapper mapper = new ProductoTiendaApiMapper();

        ProductoTiendaController controller = new ProductoTiendaController(productoTiendaService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelvePrecioComoTexto() throws Exception {
        when(productoTiendaService.listarPorProducto(1L)).thenReturn(List.of(
                new ProductoTiendaResumen(1L, 1L, 1L, new BigDecimal("10.50"), BigDecimal.ZERO,
                        new BigDecimal("100"), true, true, true)));

        mockMvc.perform(get("/api/v1/productos/1/tiendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precioVenta").value("10.50"));
    }

    @Test
    void asignarDevuelve201() throws Exception {
        when(productoTiendaService.asignar(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new ProductoTiendaResumen(2L, 1L, 5L, BigDecimal.TEN, BigDecimal.ZERO,
                        new BigDecimal("50"), true, true, true));

        mockMvc.perform(post("/api/v1/productos/1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaId\":5,\"precioVenta\":10,\"stockMinimo\":0,\"stockMaximo\":50,"
                                + "\"permitirVenta\":true,\"permitirIngreso\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tiendaId").value(5));
    }

    @Test
    void asignarDuplicadoDevuelve409() throws Exception {
        when(productoTiendaService.asignar(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenThrow(new ConfiguracionTiendaDuplicadaException(1L, 5L));

        mockMvc.perform(post("/api/v1/productos/1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaId\":5,\"precioVenta\":10,\"stockMinimo\":0,\"stockMaximo\":50,"
                                + "\"permitirVenta\":true,\"permitirIngreso\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUCTO_TIENDA_DUPLICADO"));
    }

    @Test
    void activarYDesactivarDevuelven204() throws Exception {
        mockMvc.perform(post("/api/v1/productos/1/tiendas/2/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/productos/1/tiendas/2/activar")).andExpect(status().isNoContent());

        verify(productoTiendaService).desactivar(2L);
        verify(productoTiendaService).activar(2L);
    }
}
