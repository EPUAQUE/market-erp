package com.ais.marketbackend.productos.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.productos.api.dtos.responses.ProductoResponse;
import com.ais.marketbackend.productos.api.mappers.ProductoApiMapper;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.domain.exception.ProductoDuplicadoException;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductoControllerTest {

    private ProductoService productoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productoService = mock(ProductoService.class);
        ProductoApiMapper mapper = resumen -> ProductoResponse.builder()
                .id(resumen.id())
                .codigoInterno(resumen.codigoInterno())
                .codigoBarras(resumen.codigoBarras())
                .nombre(resumen.nombre())
                .descripcion(resumen.descripcion())
                .categoriaId(resumen.categoriaId())
                .marcaId(resumen.marcaId())
                .unidadMedidaId(resumen.unidadMedidaId())
                .imagenUrl(resumen.imagenUrl())
                .activo(resumen.activo())
                .build();

        ProductoController controller = new ProductoController(productoService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLosProductosPaginados() throws Exception {
        when(productoService.listar(0, 20)).thenReturn(new Pagina<>(
                List.of(new ProductoResumen(1L, "P001", null, "Leche", null, 1L, 2L, 3L, null, true)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].codigoInterno").value("P001"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(productoService.crear(anyString(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ProductoResumen(2L, "P002", null, "Azúcar", null, 1L, 2L, 3L, null, true));

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoInterno\":\"P002\",\"nombre\":\"Azúcar\",\"categoriaId\":1,"
                                + "\"marcaId\":2,\"unidadMedidaId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoInterno").value("P002"));
    }

    @Test
    void crearDuplicadoDevuelve409() throws Exception {
        when(productoService.crear(anyString(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenThrow(new ProductoDuplicadoException("P001"));

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoInterno\":\"P001\",\"nombre\":\"Leche\",\"categoriaId\":1,"
                                + "\"marcaId\":2,\"unidadMedidaId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUCTO_DUPLICADO"));
    }

    @Test
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoInterno\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
