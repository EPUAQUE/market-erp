package com.ais.marketbackend.notificaciones.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import com.ais.marketbackend.notificaciones.application.services.impl.NotificacionServiceImpl;
import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.domain.repository.NotificacionRepository;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificacionServiceImplTest {

    private NotificacionRepository notificacionRepository;
    private CuentaPorPagarService cuentaPorPagarService;
    private CuentaPorCobrarService cuentaPorCobrarService;
    private GastoProgramadoService gastoProgramadoService;
    private ProductoTiendaService productoTiendaService;
    private InventarioService inventarioService;
    private NotificacionServiceImpl notificacionService;

    @BeforeEach
    void setUp() {
        notificacionRepository = mock(NotificacionRepository.class);
        cuentaPorPagarService = mock(CuentaPorPagarService.class);
        cuentaPorCobrarService = mock(CuentaPorCobrarService.class);
        gastoProgramadoService = mock(GastoProgramadoService.class);
        productoTiendaService = mock(ProductoTiendaService.class);
        inventarioService = mock(InventarioService.class);
        notificacionService = new NotificacionServiceImpl(
                notificacionRepository, cuentaPorPagarService, cuentaPorCobrarService, gastoProgramadoService,
                productoTiendaService, inventarioService);

        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of());
        when(cuentaPorCobrarService.listarPorTienda(1L)).thenReturn(List.of());
        when(gastoProgramadoService.listarPorTienda(1L)).thenReturn(List.of());
        when(productoTiendaService.listarPorTienda(1L)).thenReturn(List.of());
        when(inventarioService.listarPorTienda(1L)).thenReturn(List.of());
        when(notificacionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void generarCreaNotificacionParaCuentaPorPagarVencida() {
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of(cuentaPorPagar(9L,
                EstadoCuentaPorPagar.PENDIENTE, Instant.now().minus(1, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).hasSize(1);
        assertThat(creadas.get(0).tipo()).isEqualTo(TipoNotificacion.CUENTA_POR_PAGAR_VENCIDA);
        assertThat(creadas.get(0).referenciaId()).isEqualTo(9L);
    }

    @Test
    void generarNoCreaNotificacionParaCuentaPorPagarAunNoVencida() {
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of(cuentaPorPagar(9L,
                EstadoCuentaPorPagar.PENDIENTE, Instant.now().plus(30, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).isEmpty();
    }

    @Test
    void generarNoCreaNotificacionParaCuentaPorPagarYaPagada() {
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of(cuentaPorPagar(9L,
                EstadoCuentaPorPagar.PAGADA, Instant.now().minus(1, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).isEmpty();
    }

    @Test
    void generarCreaNotificacionParaCuentaPorCobrarVencida() {
        when(cuentaPorCobrarService.listarPorTienda(1L)).thenReturn(List.of(cuentaPorCobrar(9L,
                EstadoCuentaPorCobrar.PENDIENTE, Instant.now().minus(1, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).hasSize(1);
        assertThat(creadas.get(0).tipo()).isEqualTo(TipoNotificacion.CUENTA_POR_COBRAR_VENCIDA);
    }

    @Test
    void generarCreaNotificacionParaGastoProgramadoVencido() {
        when(gastoProgramadoService.listarPorTienda(1L)).thenReturn(List.of(gastoProgramado(9L, true,
                Instant.now().minus(1, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).hasSize(1);
        assertThat(creadas.get(0).tipo()).isEqualTo(TipoNotificacion.GASTO_PROGRAMADO_VENCIDO);
    }

    @Test
    void generarNoCreaNotificacionParaGastoProgramadoInactivo() {
        when(gastoProgramadoService.listarPorTienda(1L)).thenReturn(List.of(gastoProgramado(9L, false,
                Instant.now().minus(1, ChronoUnit.DAYS))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).isEmpty();
    }

    @Test
    void generarCreaNotificacionDeStockBajoCuandoExistenciaEsMenorAlMinimo() {
        when(productoTiendaService.listarPorTienda(1L)).thenReturn(List.of(
                new ProductoTiendaResumen(5L, 20L, 1L, new BigDecimal("10.00"), new BigDecimal("5.000"),
                        new BigDecimal("50.000"), true, true, true)));
        when(inventarioService.listarPorTienda(1L)).thenReturn(List.of(
                new InventarioResumen(1L, 1L, 20L, new BigDecimal("2.000"), new BigDecimal("4.0000"))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).hasSize(1);
        assertThat(creadas.get(0).tipo()).isEqualTo(TipoNotificacion.STOCK_BAJO);
        assertThat(creadas.get(0).referenciaId()).isEqualTo(20L);
    }

    @Test
    void generarNoCreaNotificacionDeStockBajoCuandoExistenciaCubreElMinimo() {
        when(productoTiendaService.listarPorTienda(1L)).thenReturn(List.of(
                new ProductoTiendaResumen(5L, 20L, 1L, new BigDecimal("10.00"), new BigDecimal("5.000"),
                        new BigDecimal("50.000"), true, true, true)));
        when(inventarioService.listarPorTienda(1L)).thenReturn(List.of(
                new InventarioResumen(1L, 1L, 20L, new BigDecimal("10.000"), new BigDecimal("4.0000"))));

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).isEmpty();
    }

    @Test
    void generarNoDuplicaUnaNotificacionYaCreadaYSinLeer() {
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of(cuentaPorPagar(9L,
                EstadoCuentaPorPagar.PENDIENTE, Instant.now().minus(1, ChronoUnit.DAYS))));
        when(notificacionRepository.existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(
                1L, TipoNotificacion.CUENTA_POR_PAGAR_VENCIDA, 9L)).thenReturn(true);

        List<NotificacionResumen> creadas = notificacionService.generar(1L);

        assertThat(creadas).isEmpty();
    }

    @Test
    void marcarLeidaDeOtraTiendaLanzaNoEncontrada() {
        Notificacion notificacion = withId(Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, "x"), 5L);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(notificacion));

        assertThatThrownBy(() -> notificacionService.marcarLeida(99L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarLeidaMarcaLaNotificacionComoLeida() {
        Notificacion notificacion = withId(Notificacion.nueva(1L, TipoNotificacion.STOCK_BAJO, 9L, "x"), 5L);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(notificacion));

        NotificacionResumen resumen = notificacionService.marcarLeida(1L, 5L);

        assertThat(resumen.leida()).isTrue();
    }

    private CuentaPorPagarResumen cuentaPorPagar(Long id, EstadoCuentaPorPagar estado, Instant fechaVencimiento) {
        return new CuentaPorPagarResumen(
                id, 1L, 1L, 1L, Instant.now().minus(31, ChronoUnit.DAYS), fechaVencimiento,
                new BigDecimal("100.00"), new BigDecimal("100.00"), estado, List.of());
    }

    private CuentaPorCobrarResumen cuentaPorCobrar(Long id, EstadoCuentaPorCobrar estado, Instant fechaVencimiento) {
        return new CuentaPorCobrarResumen(
                id, 1L, 1L, 1L, Instant.now().minus(31, ChronoUnit.DAYS), fechaVencimiento,
                new BigDecimal("100.00"), new BigDecimal("100.00"), estado, List.of());
    }

    private GastoProgramadoResumen gastoProgramado(Long id, boolean activo, Instant proximaFecha) {
        return new GastoProgramadoResumen(
                id, 1L, "Renta", new BigDecimal("100.00"), FrecuenciaGasto.MENSUAL, proximaFecha, activo, List.of());
    }

    private Notificacion withId(Notificacion notificacion, Long id) {
        return new Notificacion(
                id, notificacion.getTiendaId(), notificacion.getTipo(), notificacion.getReferenciaId(),
                notificacion.getMensaje(), notificacion.getFecha(), notificacion.isLeida(),
                notificacion.getFechaLectura());
    }
}
