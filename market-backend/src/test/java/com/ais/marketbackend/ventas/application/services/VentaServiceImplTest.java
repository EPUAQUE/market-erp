package com.ais.marketbackend.ventas.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.clientes.domain.model.EstadoCliente;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.PagoInmediato;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.impl.VentaServiceImpl;
import com.ais.marketbackend.ventas.domain.exception.DesglosePagoInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.EstadoVentaInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.LimiteCreditoExcedidoException;
import com.ais.marketbackend.ventas.domain.model.LineaVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import com.ais.marketbackend.ventas.domain.model.Venta;
import com.ais.marketbackend.ventas.domain.repository.VentaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VentaServiceImplTest {

    private VentaRepository ventaRepository;
    private InventarioService inventarioService;
    private CuentaPorCobrarService cuentaPorCobrarService;
    private ClienteService clienteService;
    private CajaService cajaService;
    private VentaServiceImpl ventaService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        inventarioService = mock(InventarioService.class);
        cuentaPorCobrarService = mock(CuentaPorCobrarService.class);
        clienteService = mock(ClienteService.class);
        cajaService = mock(CajaService.class);
        ventaService = new VentaServiceImpl(
                ventaRepository, inventarioService, cuentaPorCobrarService, clienteService, cajaService);
        when(ventaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaPorCobrarService.listarPorTienda(any())).thenReturn(List.of());
    }

    private ClienteResumen cliente(BigDecimal limiteCredito) {
        return new ClienteResumen(2L, null, "Cliente de prueba", null, null, null, EstadoCliente.ACTIVO, limiteCredito);
    }

    private CuentaPorCobrarResumen cuentaPendiente(Long clienteId, BigDecimal saldoPendiente) {
        return new CuentaPorCobrarResumen(
                99L, 1L, clienteId, 1L, java.time.Instant.now(), java.time.Instant.now(), saldoPendiente,
                saldoPendiente, EstadoCuentaPorCobrar.PENDIENTE, List.of());
    }

    @Test
    void crearConstruyeVentaEnBorrador() {
        VentaResumen resumen = ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, new BigDecimal("10"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, null);

        assertThat(resumen.tiendaId()).isEqualTo(1L);
        assertThat(resumen.clienteId()).isEqualTo(2L);
        assertThat(resumen.total()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resumen.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
    }

    @Test
    void crearSinCorrelationIdNuncaConsultaPorCorrelationId() {
        ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, new BigDecimal("10"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, null);

        verify(ventaRepository, never()).findByTiendaIdAndVendedorIdAndCorrelationId(any(), any(), any());
    }

    @Test
    void crearConCorrelationIdYaExistenteYMismoContenidoDevuelveLaVentaExistenteSinCrearOtra() {
        Venta existente = withId(Venta.nueva(2L, 1L, 3L, List.of(
                LineaVenta.nueva(1L, BigDecimal.ONE, new BigDecimal("8.00"))), MetodoPago.EFECTIVO, "corr-1"), 9L, 1L);
        when(ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(1L, 3L, "corr-1"))
                .thenReturn(Optional.of(existente));

        VentaResumen resumen = ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, BigDecimal.ONE, new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "corr-1");

        assertThat(resumen.id()).isEqualTo(9L);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearConCorrelationIdYaExistenteEnOtraTiendaCreaUnaNuevaVentaIndependiente() {
        when(ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(1L, 3L, "corr-1"))
                .thenReturn(Optional.empty());

        VentaResumen resumen = ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, BigDecimal.ONE, new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "corr-1");

        assertThat(resumen.tiendaId()).isEqualTo(1L);
        verify(ventaRepository).save(argThat(v -> "corr-1".equals(v.getCorrelationId())));
    }

    @Test
    void crearConCorrelationIdReutilizadoConContenidoDistintoLanzaConflicto() {
        Venta existente = withId(Venta.nueva(2L, 1L, 3L, List.of(
                LineaVenta.nueva(1L, BigDecimal.ONE, new BigDecimal("8.00"))), MetodoPago.EFECTIVO, "corr-1"), 9L, 1L);
        when(ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(1L, 3L, "corr-1"))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, new BigDecimal("999"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "corr-1"))
                .isInstanceOf(com.ais.marketbackend.ventas.domain.exception.CorrelationIdReutilizadoException.class);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crearConCorrelationIdEnBlancoSeTrataComoSinCorrelationId() {
        ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, new BigDecimal("10"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "   ");

        verify(ventaRepository, never()).findByTiendaIdAndVendedorIdAndCorrelationId(any(), any(), any());
        verify(ventaRepository).save(argThat(v -> v.getCorrelationId() == null));
    }

    @Test
    void crearConColisionDeInsercionConcurrenteReleeYDevuelveLaVentaIdempotente() {
        Venta existente = withId(Venta.nueva(2L, 1L, 3L, List.of(
                LineaVenta.nueva(1L, BigDecimal.ONE, new BigDecimal("8.00"))), MetodoPago.EFECTIVO, "corr-1"), 9L, 1L);
        when(ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(1L, 3L, "corr-1"))
                .thenReturn(Optional.empty(), Optional.of(existente));
        when(ventaRepository.save(any())).thenThrow(
                new com.ais.marketbackend.ventas.domain.exception.ReferenciaInvalidaException("colisión"));

        VentaResumen resumen = ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, BigDecimal.ONE, new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "corr-1");

        assertThat(resumen.id()).isEqualTo(9L);
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(
                LineaVenta.nueva(1L, BigDecimal.ONE, new BigDecimal("8.00"))), MetodoPago.EFECTIVO), 9L, 1L);
        when(ventaRepository.findByTiendaId(1L, 0, 20)).thenReturn(new Pagina<>(List.of(venta), 0, 20, 1, 1));

        Pagina<VentaResumen> resultado = ventaService.listarPorTienda(1L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).id()).isEqualTo(9L);
        assertThat(resultado.totalElementos()).isEqualTo(1);
    }

    @Test
    void crearConCorrelationIdNuevoLaGuardaEnLaVenta() {
        VentaResumen resumen = ventaService.crear(
                1L, 2L, 3L, List.of(new NuevaLineaVenta(1L, new BigDecimal("10"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO, "corr-2");

        assertThat(resumen.tiendaId()).isEqualTo(1L);
        verify(ventaRepository).save(argThat(v -> "corr-2".equals(v.getCorrelationId())));
    }

    @Test
    void completarRegistraUnMovimientoDeVentaConElCostoPromedioVigente() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(
                LineaVenta.nueva(10L, new BigDecimal("2"), new BigDecimal("8.00"))), MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));

        ventaService.completar(1L, 5L);

        verify(inventarioService).registrarMovimiento(
                1L, 10L, new BigDecimal("2"), new BigDecimal("5.0000"), TipoMovimiento.VENTA);
    }

    @Test
    void completarEfectivoNoCreaCuentaPorCobrarYRegistraIngresoEnCaja() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, new BigDecimal("2"), new BigDecimal("8.00"))),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));

        ventaService.completar(1L, 5L);

        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
        verify(cajaService).registrarMovimientoSiHayAbierta(
                eq(1L), eq(TipoMovimientoCaja.INGRESO), any(), eq(new BigDecimal("16.0000")));
    }

    @Test
    void completarTarjetaNoCreaCuentaPorCobrarYRegistraIngresoEnCaja() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, new BigDecimal("2"), new BigDecimal("8.00"))),
                MetodoPago.TARJETA), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));

        ventaService.completar(1L, 5L);

        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
        verify(cajaService).registrarMovimientoSiHayAbierta(
                eq(1L), eq(TipoMovimientoCaja.INGRESO), any(), eq(new BigDecimal("16.0000")));
    }

    @Test
    void completarCreditoNoRegistraIngresoEnCajaYCreaCuentaPorElTotal() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, new BigDecimal("2"), new BigDecimal("8.00"))),
                MetodoPago.CREDITO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));
        when(clienteService.obtener(2L)).thenReturn(cliente(null));

        ventaService.completar(1L, 5L);

        verify(cajaService, never()).registrarMovimientoSiHayAbierta(any(), any(), any(), any());
        verify(cuentaPorCobrarService).crear(5L, 2L, 1L, new BigDecimal("16.0000"));
    }

    @Test
    void completarMixtaConDesgloseParcialRegistraIngresosYCreaCuentaSoloPorElSaldo() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("100")));

        ventaService.completar(1L, 5L, List.of(
                new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("5.00")),
                new PagoInmediato(MetodoPago.TARJETA, new BigDecimal("3.00"))));

        verify(cajaService).registrarMovimientoSiHayAbierta(
                eq(1L), eq(TipoMovimientoCaja.INGRESO), any(), eq(new BigDecimal("5.00")));
        verify(cajaService).registrarMovimientoSiHayAbierta(
                eq(1L), eq(TipoMovimientoCaja.INGRESO), any(), eq(new BigDecimal("3.00")));
        verify(cuentaPorCobrarService).crear(5L, 2L, 1L, new BigDecimal("0.5000"));
    }

    @Test
    void completarMixtaCubiertaPorCompletoNoCreaCuentaPorCobrar() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("100")));

        ventaService.completar(1L, 5L, List.of(
                new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("5.00")),
                new PagoInmediato(MetodoPago.TARJETA, new BigDecimal("3.50"))));

        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void completarMixtaSinDesgloseLanzaDesgloseInvalido() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.completar(1L, 5L, List.of()))
                .isInstanceOf(DesglosePagoInvalidoException.class);
        assertThat(venta.getEstado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.BORRADOR);
    }

    @Test
    void completarMixtaConCanalCreditoEnElDesgloseLanzaDesgloseInvalido() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.completar(
                1L, 5L, List.of(new PagoInmediato(MetodoPago.CREDITO, new BigDecimal("8.50")))))
                .isInstanceOf(DesglosePagoInvalidoException.class);
    }

    @Test
    void completarMixtaConSumaMayorAlTotalLanzaDesgloseInvalido() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.completar(
                1L, 5L, List.of(new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("100")))))
                .isInstanceOf(DesglosePagoInvalidoException.class);
    }

    @Test
    void completarIgnoraElDesgloseDelClienteParaMetodosNoMixtos() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, new BigDecimal("8.50"))),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("5.0000")));

        ventaService.completar(1L, 5L, List.of(new PagoInmediato(MetodoPago.TARJETA, new BigDecimal("1.00"))));

        verify(cajaService).registrarMovimientoSiHayAbierta(
                eq(1L), eq(TipoMovimientoCaja.INGRESO), any(), eq(new BigDecimal("8.5000")));
        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void completarCuandoInventarioRechazaNoCreaLaCuentaPorCobrar() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(eq(1L), eq(10L))).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.ZERO, BigDecimal.ZERO));
        org.mockito.Mockito.doThrow(new StockInsuficienteException(10L, 1L))
                .when(inventarioService).registrarMovimiento(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> ventaService.completar(1L, 5L)).isInstanceOf(StockInsuficienteException.class);
        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void completarUnaVentaDeOtraTiendaLanzaNoEncontrada() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.completar(99L, 5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any());
    }

    @Test
    void completarCuandoInventarioRechazaPorStockInsuficientePropagaLaExcepcion() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(eq(1L), eq(10L))).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.ZERO, BigDecimal.ZERO));
        org.mockito.Mockito.doThrow(new StockInsuficienteException(10L, 1L))
                .when(inventarioService).registrarMovimiento(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> ventaService.completar(1L, 5L)).isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void completarCuandoVentaNoPermitidaPropagaLaExcepcion() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(eq(1L), eq(10L))).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));
        org.mockito.Mockito.doThrow(new MovimientoNoPermitidoException("no permitido"))
                .when(inventarioService).registrarMovimiento(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> ventaService.completar(1L, 5L)).isInstanceOf(MovimientoNoPermitidoException.class);
    }

    @Test
    void anularConIdInexistenteLanzaNoEncontrado() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.anular(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anularUnaVentaYaCompletadaLanzaEstadoInvalido() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        venta.completar();
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaService.anular(1L, 5L)).isInstanceOf(EstadoVentaInvalidoException.class);
    }

    @Test
    void completarVentaACreditoDentroDelLimitePermite() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.CREDITO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("100")));

        VentaResumen resumen = ventaService.completar(1L, 5L);

        assertThat(resumen.estado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.COMPLETADA);
        verify(cuentaPorCobrarService).crear(5L, 2L, 1L, new BigDecimal("10.0000"));
    }

    @Test
    void completarVentaACreditoQueExcedeElLimiteLanzaExcepcionYNoMutaNada() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.CREDITO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("5")));

        assertThatThrownBy(() -> ventaService.completar(1L, 5L))
                .isInstanceOf(LimiteCreditoExcedidoException.class);

        assertThat(venta.getEstado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.BORRADOR);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any());
        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void completarVentaACreditoSinLimiteDefinidoNoRestringe() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.CREDITO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));
        when(clienteService.obtener(2L)).thenReturn(cliente(null));

        VentaResumen resumen = ventaService.completar(1L, 5L);

        assertThat(resumen.estado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.COMPLETADA);
    }

    @Test
    void completarVentaACreditoSumaSaldoPendienteExistenteDelCliente() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.CREDITO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("15")));
        when(cuentaPorCobrarService.listarPorTienda(1L)).thenReturn(
                List.of(cuentaPendiente(2L, new BigDecimal("10"))));

        assertThatThrownBy(() -> ventaService.completar(1L, 5L))
                .isInstanceOf(LimiteCreditoExcedidoException.class);
    }

    @Test
    void completarVentaMixtaDentroDelLimitePermite() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("100")));

        VentaResumen resumen = ventaService.completar(
                1L, 5L, List.of(new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("4"))));

        assertThat(resumen.estado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.COMPLETADA);
        verify(cuentaPorCobrarService).crear(5L, 2L, 1L, new BigDecimal("6.0000"));
    }

    @Test
    void completarVentaMixtaQueExcedeElLimiteLanzaExcepcionYNoMutaNada() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.MIXTO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(clienteService.obtener(2L)).thenReturn(cliente(new BigDecimal("5")));

        assertThatThrownBy(() -> ventaService.completar(
                1L, 5L, List.of(new PagoInmediato(MetodoPago.EFECTIVO, new BigDecimal("3")))))
                .isInstanceOf(LimiteCreditoExcedidoException.class);

        assertThat(venta.getEstado()).isEqualTo(com.ais.marketbackend.ventas.domain.model.EstadoVenta.BORRADOR);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any());
        verify(cuentaPorCobrarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void completarVentaEfectivoNoConsultaElLimiteDeCredito() {
        Venta venta = withId(Venta.nueva(2L, 1L, 3L, List.of(LineaVenta.nueva(10L, BigDecimal.TEN, BigDecimal.ONE)),
                MetodoPago.EFECTIVO), 5L, 1L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));

        ventaService.completar(1L, 5L);

        verify(clienteService, never()).obtener(any());
    }

    private Venta withId(Venta venta, Long id, Long tiendaId) {
        return new Venta(
                id, venta.getClienteId(), tiendaId, venta.getVendedorId(), venta.getFecha(), venta.getEstado(),
                venta.getLineas(), venta.getMetodoPago(), venta.getCorrelationId());
    }
}
