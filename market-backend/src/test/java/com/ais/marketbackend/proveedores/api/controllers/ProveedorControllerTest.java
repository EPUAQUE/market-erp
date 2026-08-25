package com.ais.marketbackend.proveedores.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.proveedores.api.dtos.responses.ProveedorResponse;
import com.ais.marketbackend.proveedores.api.mappers.ProveedorApiMapper;
import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import com.ais.marketbackend.proveedores.application.services.interfaces.ProveedorService;
import com.ais.marketbackend.proveedores.domain.exception.ProveedorDuplicadoException;
import com.ais.marketbackend.proveedores.domain.model.EstadoProveedor;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProveedorControllerTest {

    private ProveedorService proveedorService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proveedorService = mock(ProveedorService.class);
        ProveedorApiMapper mapper = resumen -> ProveedorResponse.builder()
                .id(resumen.id())
                .nit(resumen.nit())
                .nombre(resumen.nombre())
                .direccion(resumen.direccion())
                .telefono(resumen.telefono())
                .correo(resumen.correo())
                .estado(resumen.estado())
                .build();

        ProveedorController controller = new ProveedorController(proveedorService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLosProveedores() throws Exception {
        when(proveedorService.listar()).thenReturn(List.of(new ProveedorResumen(
                1L, "12345678-9", "Distribuidora XYZ", "Zona 1", "1234-5678", "c@x.com", EstadoProveedor.ACTIVO)));

        mockMvc.perform(get("/api/v1/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nit").value("12345678-9"));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(proveedorService.crear(anyString(), anyString(), any(), any(), any()))
                .thenReturn(new ProveedorResumen(2L, "98765432-1", "Proveedor Norte", null, null, null, EstadoProveedor.ACTIVO));

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nit\":\"98765432-1\",\"nombre\":\"Proveedor Norte\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nit").value("98765432-1"));
    }

    @Test
    void crearConNitDuplicadoDevuelve409() throws Exception {
        when(proveedorService.crear(anyString(), anyString(), any(), any(), any()))
                .thenThrow(new ProveedorDuplicadoException("12345678-9"));

        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nit\":\"12345678-9\",\"nombre\":\"Otro\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PROVEEDOR_DUPLICADO"));
    }

    @Test
    void crearConCuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nit\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarDelegaAlServicio() throws Exception {
        when(proveedorService.actualizar(1L, "Nuevo nombre", null, null, null))
                .thenReturn(new ProveedorResumen(1L, "12345678-9", "Nuevo nombre", null, null, null, EstadoProveedor.ACTIVO));

        mockMvc.perform(put("/api/v1/proveedores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
    }

    @Test
    void activarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/proveedores/1/activar"))
                .andExpect(status().isNoContent());

        verify(proveedorService).activar(1L);
    }

    @Test
    void desactivarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/proveedores/1/desactivar"))
                .andExpect(status().isNoContent());

        verify(proveedorService).desactivar(1L);
    }
}
