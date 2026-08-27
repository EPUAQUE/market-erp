package com.ais.marketbackend.dashboard.api.controllers;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.dashboard.api.mappers.DashboardApiMapper;
import com.ais.marketbackend.dashboard.application.dtos.DashboardGrupoResumen;
import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;
import com.ais.marketbackend.dashboard.application.services.interfaces.DashboardService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {

    private DashboardService dashboardService;
    private UsuarioService usuarioService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        usuarioService = mock(UsuarioService.class);
        DashboardController controller =
                new DashboardController(dashboardService, new DashboardApiMapper(), usuarioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        when(usuarioService.obtenerPermisosEfectivosPorUsername("admin"))
                .thenReturn(new PermisosEfectivos(
                        1L, "admin", Set.of("DASHBOARD_VER", "DASHBOARD_FINANCIERO_VER"), Set.of(1L), true));
    }

    @Test
    void obtenerResumenDevuelveElResumenDeLaTienda() throws Exception {
        when(dashboardService.obtenerResumen(1L)).thenReturn(resumen(
                new BigDecimal("500.00"), 3, 1, 2, 4, true, new BigDecimal("650.00")));

        mockMvc.perform(get("/api/v1/dashboard/tiendas/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasHoyTotal").value("500.00"))
                .andExpect(jsonPath("$.ventasHoyCantidad").value(3))
                .andExpect(jsonPath("$.cuentasPorCobrarVencidas").value(1))
                .andExpect(jsonPath("$.cuentasPorPagarVencidas").value(2))
                .andExpect(jsonPath("$.productosBajoMinimo").value(4))
                .andExpect(jsonPath("$.cajaAbierta").value(true))
                .andExpect(jsonPath("$.cajaSaldoEsperado").value("650.00"));
    }

    @Test
    void obtenerResumenConCajaCerradaNoIncluyeSaldoEsperado() throws Exception {
        when(dashboardService.obtenerResumen(1L)).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, 0, 0, false, null));

        mockMvc.perform(get("/api/v1/dashboard/tiendas/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cajaAbierta").value(false))
                .andExpect(jsonPath("$.cajaSaldoEsperado").value(nullValue()));
    }

    @Test
    void obtenerResumenOcultaUtilidadYMargenSinPermisoFinanciero() throws Exception {
        when(usuarioService.obtenerPermisosEfectivosPorUsername("admin"))
                .thenReturn(new PermisosEfectivos(1L, "admin", Set.of("DASHBOARD_VER"), Set.of(1L), false));
        when(dashboardService.obtenerResumen(1L)).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, 0, 0, false, null));

        mockMvc.perform(get("/api/v1/dashboard/tiendas/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilidadMesTotal").value(nullValue()))
                .andExpect(jsonPath("$.margenPromedioMes").value(nullValue()));
    }

    @Test
    void obtenerResumenGrupoDevuelveElResumenAgregado() throws Exception {
        when(dashboardService.obtenerResumenGrupo(5L)).thenReturn(new DashboardGrupoResumen(
                5L, List.of(1L, 2L), new BigDecimal("80.00"), 2, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, null, BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 1, 2, new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0));

        mockMvc.perform(get("/api/v1/dashboard/grupos/5").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoId").value(5))
                .andExpect(jsonPath("$.tiendaIds[0]").value(1))
                .andExpect(jsonPath("$.ventasHoyTotal").value("80.00"))
                .andExpect(jsonPath("$.tiendasConCajaAbierta").value(1))
                .andExpect(jsonPath("$.totalTiendas").value(2));
    }

    private DashboardResumen resumen(
            BigDecimal ventasHoyTotal, long ventasHoyCantidad, long cuentasPorCobrarVencidas,
            long cuentasPorPagarVencidas, long productosBajoMinimo, boolean cajaAbierta, BigDecimal cajaSaldoEsperado) {
        return new DashboardResumen(
                1L, ventasHoyTotal, ventasHoyCantidad, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0,
                BigDecimal.ZERO, null, BigDecimal.ZERO, 0, productosBajoMinimo, 0, BigDecimal.ZERO,
                cuentasPorCobrarVencidas, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                cuentasPorPagarVencidas, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, cajaAbierta,
                cajaSaldoEsperado, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
    }
}
