package com.ais.marketbackend.ventas.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.model.EstadoUsuario;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.ais.marketbackend.ventas.api.mappers.VentaApiMapper;
import com.ais.marketbackend.ventas.application.dtos.LineaVentaResumen;
import com.ais.marketbackend.ventas.application.dtos.PagoInmediato;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.exception.DesglosePagoInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.EstadoVentaInvalidoException;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VentaControllerTest {

    private VentaService ventaService;
    private UsuarioService usuarioService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ventaService = mock(VentaService.class);
        usuarioService = mock(UsuarioService.class);
        VentaController controller = new VentaController(ventaService, new VentaApiMapper(), usuarioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("vendedor1");
        when(usuarioService.obtenerPorUsername("vendedor1"))
                .thenReturn(new UsuarioResumen(3L, "vendedor1", EstadoUsuario.ACTIVO, null, null, null));
    }

    @Test
    void listarDevuelveLasVentasDeLaTiendaPaginadas() throws Exception {
        when(ventaService.listarPorTienda(1L, 0, 20)).thenReturn(
                new Pagina<>(List.of(resumen(5L, EstadoVenta.BORRADOR)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/ventas/tiendas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void crearDevuelve201() throws Exception {
        when(ventaService.crear(eq(1L), eq(2L), eq(3L), anyList(), any(), any()))
                .thenReturn(resumen(5L, EstadoVenta.BORRADOR));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":2,\"lineas\":[{\"productoId\":10,\"cantidad\":10,\"precioUnitario\":8.00}],"
                                + "\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crearConCorrelationIdLoPasaAlServicio() throws Exception {
        when(ventaService.crear(eq(1L), eq(2L), eq(3L), anyList(), any(), eq("corr-abc")))
                .thenReturn(resumen(5L, EstadoVenta.BORRADOR));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":2,\"lineas\":[{\"productoId\":10,\"cantidad\":10,\"precioUnitario\":8.00}],"
                                + "\"metodoPago\":\"EFECTIVO\",\"correlationId\":\"corr-abc\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void buscarPorCorrelationIdExistenteDevuelve200() throws Exception {
        when(ventaService.buscarPorCorrelationId(1L, 3L, "corr-abc"))
                .thenReturn(java.util.Optional.of(resumen(5L, EstadoVenta.BORRADOR)));

        mockMvc.perform(get("/api/v1/ventas/tiendas/1/correlation/corr-abc").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void buscarPorCorrelationIdInexistenteDevuelve404() throws Exception {
        when(ventaService.buscarPorCorrelationId(1L, 3L, "corr-x")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/ventas/tiendas/1/correlation/corr-x").principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearSinLineasDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ventas/tiendas/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":2,\"lineas\":[],\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinMetodoPagoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/ventas/tiendas/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":2,\"lineas\":[{\"productoId\":10,\"cantidad\":10,\"precioUnitario\":8.00}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completarDevuelveLaVentaActualizada() throws Exception {
        when(ventaService.completar(1L, 5L, List.of())).thenReturn(resumen(5L, EstadoVenta.COMPLETADA));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1/5/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }

    @Test
    void completarConStockInsuficienteDevuelve409() throws Exception {
        when(ventaService.completar(1L, 5L, List.of())).thenThrow(new StockInsuficienteException(10L, 1L));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1/5/completar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STOCK_INSUFICIENTE"));
    }

    @Test
    void completarMixtoEnviaElDesgloseDePagosAlServicio() throws Exception {
        when(ventaService.completar(eq(1L), eq(5L), anyList())).thenReturn(resumen(5L, EstadoVenta.COMPLETADA));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1/5/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagos\":[{\"metodoPago\":\"EFECTIVO\",\"monto\":5.00},"
                                + "{\"metodoPago\":\"TARJETA\",\"monto\":3.50}]}"))
                .andExpect(status().isOk());

        verify(ventaService).completar(eq(1L), eq(5L), eq(List.of(
                new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("5.00")),
                new PagoInmediato(MetodoPago.TARJETA, new BigDecimal("3.50")))));
    }

    @Test
    void completarConDesgloseInvalidoDevuelve400() throws Exception {
        when(ventaService.completar(eq(1L), eq(5L), anyList()))
                .thenThrow(new DesglosePagoInvalidoException("La suma excede el total."));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1/5/completar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagos\":[{\"metodoPago\":\"EFECTIVO\",\"monto\":999.00}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DESGLOSE_PAGO_INVALIDO"));
    }

    @Test
    void anularConEstadoInvalidoDevuelve400() throws Exception {
        when(ventaService.anular(1L, 5L)).thenThrow(new EstadoVentaInvalidoException(EstadoVenta.COMPLETADA));

        mockMvc.perform(post("/api/v1/ventas/tiendas/1/5/anular"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ESTADO_VENTA_INVALIDO"));
    }

    private VentaResumen resumen(Long id, EstadoVenta estado) {
        List<LineaVentaResumen> lineas = List.of(new LineaVentaResumen(1L, 10L, new BigDecimal("10"), new BigDecimal("8.00")));
        return new VentaResumen(
                id, 2L, 1L, 3L, Instant.parse("2026-01-01T00:00:00Z"), estado, lineas, new BigDecimal("80.00"),
                MetodoPago.EFECTIVO);
    }
}
