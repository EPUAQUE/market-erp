package com.ais.marketbackend.dashboard.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;
import com.ais.marketbackend.dashboard.application.services.impl.DashboardServiceImpl;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.notificaciones.application.services.interfaces.NotificacionService;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceImplTest {

    private VentaService ventaService;
    private CuentaPorCobrarService cuentaPorCobrarService;
    private CuentaPorPagarService cuentaPorPagarService;
    private ProductoTiendaService productoTiendaService;
    private InventarioService inventarioService;
    private CajaService cajaService;
    private FelService felService;
    private NotificacionService notificacionService;
    private GastoProgramadoService gastoProgramadoService;
    private TiendaService tiendaService;
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        ventaService = mock(VentaService.class);
        cuentaPorCobrarService = mock(CuentaPorCobrarService.class);
        cuentaPorPagarService = mock(CuentaPorPagarService.class);
        productoTiendaService = mock(ProductoTiendaService.class);
        inventarioService = mock(InventarioService.class);
        cajaService = mock(CajaService.class);
        felService = mock(FelService.class);
        notificacionService = mock(NotificacionService.class);
        gastoProgramadoService = mock(GastoProgramadoService.class);
        tiendaService = mock(TiendaService.class);
        dashboardService = new DashboardServiceImpl(
                ventaService, cuentaPorCobrarService, cuentaPorPagarService, productoTiendaService,
                inventarioService, cajaService, felService, notificacionService, gastoProgramadoService,
                tiendaService, "America/Guatemala");

        when(ventaService.listarPorTienda(1L)).thenReturn(List.of());
        when(cuentaPorCobrarService.listarPorTienda(1L)).thenReturn(List.of());
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of());
        when(productoTiendaService.listarPorTienda(1L)).thenReturn(List.of());
        when(inventarioService.listarPorTienda(1L)).thenReturn(List.of());
        when(cajaService.obtenerAbierta(1L)).thenThrow(new ResourceNotFoundException("No hay una caja abierta."));
        when(cajaService.listarPorTienda(1L)).thenReturn(List.of());
        when(felService.listarPorTienda(1L)).thenReturn(List.of());
        when(notificacionService.listarNoLeidasPorTienda(1L)).thenReturn(List.of());
        when(gastoProgramadoService.listarPorTienda(1L)).thenReturn(List.of());
        when(tiendaService.listar()).thenReturn(List.of());
    }

    @Test
    void sumaSoloLasVentasCompletadasDeHoy() {
        when(ventaService.listarPorTienda(1L)).thenReturn(List.of(
                venta(EstadoVenta.COMPLETADA, Instant.now(), new BigDecimal("50.00")),
                venta(EstadoVenta.COMPLETADA, Instant.now().minus(2, ChronoUnit.DAYS), new BigDecimal("999.00")),
                venta(EstadoVenta.BORRADOR, Instant.now(), new BigDecimal("30.00"))));

        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.ventasHoyTotal()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(resumen.ventasHoyCantidad()).isEqualTo(1);
    }

    @Test
    void sumaElSaldoPendienteYCuentaVencidasDeCuentasPorCobrar() {
        when(cuentaPorCobrarService.listarPorTienda(1L)).thenReturn(List.of(
                cuentaPorCobrar(EstadoCuentaPorCobrar.PENDIENTE, Instant.now().minus(1, ChronoUnit.DAYS),
                        new BigDecimal("100.00")),
                cuentaPorCobrar(EstadoCuentaPorCobrar.PENDIENTE, Instant.now().plus(10, ChronoUnit.DAYS),
                        new BigDecimal("50.00")),
                cuentaPorCobrar(EstadoCuentaPorCobrar.COBRADA, Instant.now().minus(1, ChronoUnit.DAYS),
                        new BigDecimal("0.00"))));

        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.saldoPendienteCuentasPorCobrar()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(resumen.cuentasPorCobrarVencidas()).isEqualTo(1);
    }

    @Test
    void sumaElSaldoPendienteYCuentaVencidasDeCuentasPorPagar() {
        when(cuentaPorPagarService.listarPorTienda(1L)).thenReturn(List.of(
                cuentaPorPagar(EstadoCuentaPorPagar.PENDIENTE, Instant.now().minus(1, ChronoUnit.DAYS),
                        new BigDecimal("200.00"))));

        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.saldoPendienteCuentasPorPagar()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(resumen.cuentasPorPagarVencidas()).isEqualTo(1);
    }

    @Test
    void cuentaLosProductosConExistenciaMenorAlStockMinimo() {
        when(productoTiendaService.listarPorTienda(1L)).thenReturn(List.of(
                new ProductoTiendaResumen(1L, 20L, 1L, new BigDecimal("10.00"), new BigDecimal("5.000"),
                        new BigDecimal("50.000"), true, true, true),
                new ProductoTiendaResumen(2L, 21L, 1L, new BigDecimal("10.00"), new BigDecimal("5.000"),
                        new BigDecimal("50.000"), true, true, true)));
        when(inventarioService.listarPorTienda(1L)).thenReturn(List.of(
                new InventarioResumen(1L, 1L, 20L, new BigDecimal("2.000"), new BigDecimal("4.0000")),
                new InventarioResumen(2L, 1L, 21L, new BigDecimal("10.000"), new BigDecimal("4.0000"))));

        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.productosBajoMinimo()).isEqualTo(1);
    }

    @Test
    void reportaCajaAbiertaConSuSaldoEsperado() {
        doReturn(new CajaSesionResumen(
                1L, 1L, Instant.now(), null, new BigDecimal("100.00"), null, new BigDecimal("150.00"),
                EstadoCajaSesion.ABIERTA, List.of())).when(cajaService).obtenerAbierta(1L);

        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.cajaAbierta()).isTrue();
        assertThat(resumen.cajaSaldoEsperado()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void reportaCajaCerradaCuandoNoHayCajaAbierta() {
        DashboardResumen resumen = dashboardService.obtenerResumen(1L);

        assertThat(resumen.cajaAbierta()).isFalse();
        assertThat(resumen.cajaSaldoEsperado()).isNull();
    }

    private VentaResumen venta(EstadoVenta estado, Instant fecha, BigDecimal total) {
        return new VentaResumen(1L, 1L, 1L, 1L, fecha, estado, List.of(), total, MetodoPago.EFECTIVO);
    }

    private CuentaPorCobrarResumen cuentaPorCobrar(
            EstadoCuentaPorCobrar estado, Instant fechaVencimiento, BigDecimal saldoPendiente) {
        return new CuentaPorCobrarResumen(
                1L, 1L, 1L, 1L, Instant.now().minus(31, ChronoUnit.DAYS), fechaVencimiento, saldoPendiente,
                saldoPendiente, estado, List.of());
    }

    private CuentaPorPagarResumen cuentaPorPagar(
            EstadoCuentaPorPagar estado, Instant fechaVencimiento, BigDecimal saldoPendiente) {
        return new CuentaPorPagarResumen(
                1L, 1L, 1L, 1L, Instant.now().minus(31, ChronoUnit.DAYS), fechaVencimiento, saldoPendiente,
                saldoPendiente, estado, List.of());
    }
}
