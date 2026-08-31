package com.ais.marketbackend.fel.api.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.fel.api.dtos.responses.DocumentoFelResponse;
import com.ais.marketbackend.fel.api.mappers.DocumentoFelApiMapper;
import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
import com.ais.marketbackend.fel.domain.exception.VentaNoCompletadaException;
import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FelControllerTest {

    private FelService felService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        felService = mock(FelService.class);
        DocumentoFelApiMapper mapper = resumen -> DocumentoFelResponse.builder()
                .id(resumen.id())
                .ventaId(resumen.ventaId())
                .tiendaId(resumen.tiendaId())
                .serie(resumen.serie())
                .numero(resumen.numero())
                .uuid(resumen.uuid())
                .estado(resumen.estado())
                .fechaEmision(resumen.fechaEmision())
                .fechaCertificacion(resumen.fechaCertificacion())
                .motivoAnulacion(resumen.motivoAnulacion())
                .mensajeError(resumen.mensajeError())
                .build();
        FelController controller = new FelController(felService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry(), (tipo, correlationId, detalle) -> { }))
                .build();
    }

    @Test
    void listarDevuelveLosDocumentosDeLaTienda() throws Exception {
        when(felService.listarPorTienda(1L)).thenReturn(List.of(resumen(EstadoDocumentoFel.CERTIFICADO)));

        mockMvc.perform(get("/api/v1/fel/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CERTIFICADO"));
    }

    @Test
    void emitirDevuelveElDocumentoCertificado() throws Exception {
        when(felService.emitir(1L, 9L)).thenReturn(resumen(EstadoDocumentoFel.CERTIFICADO));

        mockMvc.perform(post("/api/v1/fel/tiendas/1/ventas/9/emitir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-abc"));
    }

    @Test
    void emitirParaVentaNoCompletadaDevuelve400() throws Exception {
        when(felService.emitir(1L, 9L)).thenThrow(new VentaNoCompletadaException());

        mockMvc.perform(post("/api/v1/fel/tiendas/1/ventas/9/emitir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VENTA_NO_COMPLETADA"));
    }

    @Test
    void reintentarDevuelveElDocumentoActualizado() throws Exception {
        when(felService.reintentar(1L, 5L)).thenReturn(resumen(EstadoDocumentoFel.CERTIFICADO));

        mockMvc.perform(post("/api/v1/fel/tiendas/1/5/reintentar"))
                .andExpect(status().isOk());
    }

    @Test
    void anularDevuelveElDocumentoAnulado() throws Exception {
        when(felService.anular(1L, 5L, "Motivo de prueba")).thenReturn(resumen(EstadoDocumentoFel.ANULADO));

        mockMvc.perform(post("/api/v1/fel/tiendas/1/5/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Motivo de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    void anularSinMotivoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/fel/tiendas/1/5/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private DocumentoFelResumen resumen(EstadoDocumentoFel estado) {
        return new DocumentoFelResumen(
                5L, 9L, 1L, "A", 1L, estado == EstadoDocumentoFel.PENDIENTE ? null : "uuid-abc", estado,
                Instant.now(), estado == EstadoDocumentoFel.PENDIENTE ? null : Instant.now(),
                estado == EstadoDocumentoFel.ANULADO ? "Motivo de prueba" : null, null);
    }
}
