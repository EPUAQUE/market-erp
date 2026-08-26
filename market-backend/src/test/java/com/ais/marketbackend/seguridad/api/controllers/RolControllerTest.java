package com.ais.marketbackend.seguridad.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.seguridad.application.dtos.RolResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.RolService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RolControllerTest {

    private RolService rolService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rolService = mock(RolService.class);
        RolController controller = new RolController(rolService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarDevuelveLosRolesDelServicio() throws Exception {
        when(rolService.listar()).thenReturn(List.of(
                new RolResumen(1L, "ADMIN", true), new RolResumen(2L, "ENCARGADO_TIENDA", false)));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("ADMIN"))
                .andExpect(jsonPath("$[0].alcanceGlobal").value(true))
                .andExpect(jsonPath("$[1].nombre").value("ENCARGADO_TIENDA"));
    }
}
