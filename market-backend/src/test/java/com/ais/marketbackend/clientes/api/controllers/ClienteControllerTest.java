package com.ais.marketbackend.clientes.api.controllers;

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

import com.ais.marketbackend.clientes.api.dtos.responses.ClienteResponse;
import com.ais.marketbackend.clientes.api.mappers.ClienteApiMapper;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.clientes.domain.exception.ClienteDuplicadoException;
import com.ais.marketbackend.clientes.domain.model.EstadoCliente;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClienteControllerTest {

    private ClienteService clienteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clienteService = mock(ClienteService.class);
        ClienteApiMapper mapper = resumen -> ClienteResponse.builder()
                .id(resumen.id())
                .nit(resumen.nit())
                .nombre(resumen.nombre())
                .direccion(resumen.direccion())
                .telefono(resumen.telefono())
                .correo(resumen.correo())
                .estado(resumen.estado())
                .limiteCredito(resumen.limiteCredito() == null ? null : resumen.limiteCredito().toPlainString())
                .build();

        ClienteController controller = new ClienteController(clienteService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void listarDevuelveLosClientes() throws Exception {
        when(clienteService.listar()).thenReturn(List.of(new ClienteResumen(
                1L, "12345678-9", "Juan Pérez", null, null, null, EstadoCliente.ACTIVO, null)));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nit").value("12345678-9"));
    }

    @Test
    void crearSinNitDevuelve201() throws Exception {
        when(clienteService.crear(any(), anyString(), any(), any(), any(), any()))
                .thenReturn(
                        new ClienteResumen(2L, null, "Consumidor Final", null, null, null, EstadoCliente.ACTIVO, null));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consumidor Final\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nit").doesNotExist());
    }

    @Test
    void crearConNitDuplicadoDevuelve409() throws Exception {
        when(clienteService.crear(any(), anyString(), any(), any(), any(), any()))
                .thenThrow(new ClienteDuplicadoException("12345678-9"));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nit\":\"12345678-9\",\"nombre\":\"Otro\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLIENTE_DUPLICADO"));
    }

    @Test
    void crearSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarDelegaAlServicio() throws Exception {
        when(clienteService.actualizar(1L, "Nuevo nombre", null, null, null, null))
                .thenReturn(new ClienteResumen(
                        1L, "12345678-9", "Nuevo nombre", null, null, null, EstadoCliente.ACTIVO, null));

        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo nombre"));
    }

    @Test
    void activarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/1/activar"))
                .andExpect(status().isNoContent());

        verify(clienteService).activar(1L);
    }

    @Test
    void desactivarDelegaAlServicioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/1/desactivar"))
                .andExpect(status().isNoContent());

        verify(clienteService).desactivar(1L);
    }
}
