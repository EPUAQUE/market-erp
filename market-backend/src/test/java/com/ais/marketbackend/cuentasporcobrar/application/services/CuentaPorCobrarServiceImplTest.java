package com.ais.marketbackend.cuentasporcobrar.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.impl.CuentaPorCobrarServiceImpl;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CobroExcedeSaldoException;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CuentaConCobrosException;
import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import com.ais.marketbackend.cuentasporcobrar.domain.repository.CuentaPorCobrarRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuentaPorCobrarServiceImplTest {

    private CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private CajaService cajaService;
    private CuentaPorCobrarServiceImpl cuentaPorCobrarService;

    @BeforeEach
    void setUp() {
        cuentaPorCobrarRepository = mock(CuentaPorCobrarRepository.class);
        cajaService = mock(CajaService.class);
        cuentaPorCobrarService = new CuentaPorCobrarServiceImpl(cuentaPorCobrarRepository, cajaService);
        when(cuentaPorCobrarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConstruyeCuentaPendientePorElMontoTotal() {
        CuentaPorCobrarResumen resumen = cuentaPorCobrarService.crear(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThat(resumen.estado()).isEqualTo(EstadoCuentaPorCobrar.PENDIENTE);
        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByTiendaId(3L, 0, 20)).thenReturn(new Pagina<>(List.of(cuenta), 0, 20, 1, 1));

        Pagina<CuentaPorCobrarResumen> resultado = cuentaPorCobrarService.listarPorTienda(3L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).id()).isEqualTo(9L);
    }

    @Test
    void registrarCobroDeOtraTiendaLanzaNoEncontrada() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorCobrarService.registrarCobro(99L, 9L, BigDecimal.TEN, MetodoPago.EFECTIVO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarCobroReduceElSaldo() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(cuenta));

        CuentaPorCobrarResumen resumen =
                cuentaPorCobrarService.registrarCobro(3L, 9L, new BigDecimal("30.00"), MetodoPago.EFECTIVO);

        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void registrarCobroReflejaUnIngresoEnCaja() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(cuenta));

        cuentaPorCobrarService.registrarCobro(3L, 9L, new BigDecimal("30.00"), MetodoPago.EFECTIVO);

        org.mockito.Mockito.verify(cajaService).registrarMovimientoSiHayAbierta(
                3L, com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja.INGRESO,
                "Cobro cuenta por cobrar #9", new BigDecimal("30.00"));
    }

    @Test
    void registrarCobroQueExcedeElSaldoLanzaExcepcion() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(
                        () -> cuentaPorCobrarService.registrarCobro(3L, 9L, new BigDecimal("200.00"), MetodoPago.EFECTIVO))
                .isInstanceOf(CobroExcedeSaldoException.class);
    }

    @Test
    void anularConCobrosLanzaExcepcion() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        cuenta.registrarCobro(new BigDecimal("10.00"), MetodoPago.EFECTIVO);
        when(cuentaPorCobrarRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorCobrarService.anular(3L, 9L)).isInstanceOf(CuentaConCobrosException.class);
    }

    @Test
    void obtenerPorVentaDevuelveLaCuentaAsociada() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(7L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByVentaId(7L)).thenReturn(Optional.of(cuenta));

        CuentaPorCobrarResumen resumen = cuentaPorCobrarService.obtenerPorVenta(3L, 7L);

        assertThat(resumen.id()).isEqualTo(9L);
        assertThat(resumen.ventaId()).isEqualTo(7L);
    }

    @Test
    void obtenerPorVentaSinCuentaLanzaNoEncontrada() {
        when(cuentaPorCobrarRepository.findByVentaId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaPorCobrarService.obtenerPorVenta(3L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorVentaDeOtraTiendaLanzaNoEncontrada() {
        CuentaPorCobrar cuenta = withId(CuentaPorCobrar.nueva(7L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorCobrarRepository.findByVentaId(7L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorCobrarService.obtenerPorVenta(99L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anularConIdInexistenteLanzaNoEncontrado() {
        when(cuentaPorCobrarRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaPorCobrarService.anular(3L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private CuentaPorCobrar withId(CuentaPorCobrar cuenta, Long id) {
        return new CuentaPorCobrar(
                id, cuenta.getVentaId(), cuenta.getClienteId(), cuenta.getTiendaId(), cuenta.getFechaEmision(),
                cuenta.getFechaVencimiento(), cuenta.getMontoOriginal(), cuenta.getSaldoPendiente(),
                cuenta.getEstado(), cuenta.getCobros());
    }
}
