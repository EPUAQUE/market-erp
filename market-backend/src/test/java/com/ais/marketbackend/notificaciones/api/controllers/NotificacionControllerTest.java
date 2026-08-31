package com.ais.marketbackend.notificaciones.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.notificaciones.api.dtos.responses.NotificacionResponse;
import com.ais.marketbackend.notificaciones.api.mappers.NotificacionApiMapper;
import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import com.ais.marketbackend.notificaciones.application.services.interfaces.NotificacionService;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificacionControllerTest {

    private NotificacionService notificacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificacionService = mock(NotificacionService.class);
        NotificacionApiMapper mapper = resumen -> NotificacionResponse.builder()
                .id(resumen.id())
                .tiendaId(resumen.tiendaId())
                .tipo(resumen.tipo())
                .referenciaId(resumen.referenciaId())
                .mensaje(resumen.mensaje())
                .fecha(resumen.fecha())
                .leida(resumen.leida())
                .fechaLectura(resumen.fechaLectura())
                .build();
        NotificacionController controller = new NotificacionController(notificacionService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLasNotificacionesDeLaTienda() throws Exception {
        when(notificacionService.listarPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(9L, false)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/notificaciones/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].leida").value(false));
    }

    @Test
    void listarNoLeidasDevuelveSoloLasNoLeidas() throws Exception {
        when(notificacionService.listarNoLeidasPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(9L, false)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/notificaciones/tiendas/1/no-leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(9));
    }

    @Test
    void generarDevuelveLasNotificacionesCreadas() throws Exception {
        when(notificacionService.generar(1L)).thenReturn(List.of(resumen(9L, false)));

        mockMvc.perform(post("/api/v1/notificaciones/tiendas/1/generar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("STOCK_BAJO"));
    }

    @Test
    void marcarLeidaDevuelveLaNotificacionActualizada() throws Exception {
        when(notificacionService.marcarLeida(1L, 9L)).thenReturn(resumen(9L, true));

        mockMvc.perform(post("/api/v1/notificaciones/tiendas/1/9/marcar-leida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    private NotificacionResumen resumen(Long id, boolean leida) {
        return new NotificacionResumen(
                id, 1L, TipoNotificacion.STOCK_BAJO, 20L, "Stock bajo del producto #20", Instant.now(), leida,
                leida ? Instant.now() : null);
    }
}
