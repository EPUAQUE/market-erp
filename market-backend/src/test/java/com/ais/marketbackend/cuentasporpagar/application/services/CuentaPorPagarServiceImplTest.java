package com.ais.marketbackend.cuentasporpagar.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.impl.CuentaPorPagarServiceImpl;
import com.ais.marketbackend.cuentasporpagar.domain.exception.CuentaConPagosException;
import com.ais.marketbackend.cuentasporpagar.domain.exception.PagoExcedeSaldoException;
import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.cuentasporpagar.domain.repository.CuentaPorPagarRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuentaPorPagarServiceImplTest {

    private CuentaPorPagarRepository cuentaPorPagarRepository;
    private CajaService cajaService;
    private CuentaPorPagarServiceImpl cuentaPorPagarService;

    @BeforeEach
    void setUp() {
        cuentaPorPagarRepository = mock(CuentaPorPagarRepository.class);
        cajaService = mock(CajaService.class);
        cuentaPorPagarService = new CuentaPorPagarServiceImpl(cuentaPorPagarRepository, cajaService);
        when(cuentaPorPagarRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConstruyeCuentaPendientePorElMontoTotal() {
        CuentaPorPagarResumen resumen = cuentaPorPagarService.crear(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThat(resumen.estado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void registrarPagoDeOtraTiendaLanzaNoEncontrada() {
        CuentaPorPagar cuenta = withId(CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorPagarRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorPagarService.registrarPago(99L, 9L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarPagoReduceElSaldo() {
        CuentaPorPagar cuenta = withId(CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorPagarRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        CuentaPorPagarResumen resumen = cuentaPorPagarService.registrarPago(3L, 9L, new BigDecimal("30.00"));

        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void registrarPagoReflejaUnEgresoEnCaja() {
        CuentaPorPagar cuenta = withId(CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorPagarRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        cuentaPorPagarService.registrarPago(3L, 9L, new BigDecimal("30.00"));

        org.mockito.Mockito.verify(cajaService).registrarMovimientoSiHayAbierta(
                3L, com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja.EGRESO,
                "Pago cuenta por pagar #9", new BigDecimal("30.00"));
    }

    @Test
    void registrarPagoQueExcedeElSaldoLanzaExcepcion() {
        CuentaPorPagar cuenta = withId(CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        when(cuentaPorPagarRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorPagarService.registrarPago(3L, 9L, new BigDecimal("200.00")))
                .isInstanceOf(PagoExcedeSaldoException.class);
    }

    @Test
    void anularConPagosLanzaExcepcion() {
        CuentaPorPagar cuenta = withId(CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00")), 9L);
        cuenta.registrarPago(new BigDecimal("10.00"));
        when(cuentaPorPagarRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaPorPagarService.anular(3L, 9L)).isInstanceOf(CuentaConPagosException.class);
    }

    @Test
    void anularConIdInexistenteLanzaNoEncontrado() {
        when(cuentaPorPagarRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaPorPagarService.anular(3L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private CuentaPorPagar withId(CuentaPorPagar cuenta, Long id) {
        return new CuentaPorPagar(
                id, cuenta.getCompraId(), cuenta.getProveedorId(), cuenta.getTiendaId(), cuenta.getFechaEmision(),
                cuenta.getFechaVencimiento(), cuenta.getMontoOriginal(), cuenta.getSaldoPendiente(),
                cuenta.getEstado(), cuenta.getPagos());
    }
}
