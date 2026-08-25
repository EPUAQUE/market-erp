package com.ais.marketbackend.productos.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.productos.api.mappers.ProductoTiendaApiMapper;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductoCatalogoControllerTest {

    private ProductoTiendaService productoTiendaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productoTiendaService = mock(ProductoTiendaService.class);
        ProductoCatalogoController controller =
                new ProductoCatalogoController(productoTiendaService, new ProductoTiendaApiMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarDevuelveElCatalogoDeVentaDeLaTiendaPaginado() throws Exception {
        when(productoTiendaService.listarPorTienda(1L, 0, 20)).thenReturn(new Pagina<>(List.of(
                new ProductoTiendaResumen(1L, 20L, 1L, new BigDecimal("15.00"), BigDecimal.ZERO,
                        new BigDecimal("100"), true, true, true)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/productos/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].productoId").value(20))
                .andExpect(jsonPath("$.contenido[0].precioVenta").value("15.00"));
    }
}
