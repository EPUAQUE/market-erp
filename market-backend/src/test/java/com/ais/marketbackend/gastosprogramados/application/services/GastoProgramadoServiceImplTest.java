package com.ais.marketbackend.gastosprogramados.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.services.impl.GastoProgramadoServiceImpl;
import com.ais.marketbackend.gastosprogramados.domain.exception.GastoNoVencidoException;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import com.ais.marketbackend.gastosprogramados.domain.repository.GastoProgramadoRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GastoProgramadoServiceImplTest {

    private GastoProgramadoRepository gastoProgramadoRepository;
    private CajaService cajaService;
    private GastoProgramadoServiceImpl gastoProgramadoService;

    @BeforeEach
    void setUp() {
        gastoProgramadoRepository = mock(GastoProgramadoRepository.class);
        cajaService = mock(CajaService.class);
        gastoProgramadoService = new GastoProgramadoServiceImpl(gastoProgramadoRepository, cajaService);
        when(gastoProgramadoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConstruyeUnGastoActivo() {
        GastoProgramadoResumen resumen = gastoProgramadoService.crear(
                1L, "Renta local", new BigDecimal("1500.00"), FrecuenciaGasto.MENSUAL,
                Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(resumen.activo()).isTrue();
        assertThat(resumen.monto()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void generarPagoDeOtraTiendaLanzaNoEncontrado() {
        GastoProgramado gasto = withId(GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2026-01-01T00:00:00Z")), 9L);
        when(gastoProgramadoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(gasto));

        assertThatThrownBy(() -> gastoProgramadoService.generarPago(99L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generarPagoAntesDeVencerLanzaGastoNoVencido() {
        GastoProgramado gasto = withId(GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.now().plusSeconds(3600)), 9L);
        when(gastoProgramadoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(gasto));

        assertThatThrownBy(() -> gastoProgramadoService.generarPago(1L, 9L))
                .isInstanceOf(GastoNoVencidoException.class);
    }

    @Test
    void generarPagoReflejaUnEgresoEnCaja() {
        GastoProgramado gasto = withId(GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2020-01-01T00:00:00Z")), 9L);
        when(gastoProgramadoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(gasto));

        gastoProgramadoService.generarPago(1L, 9L);

        verify(cajaService).registrarMovimientoSiHayAbierta(
                1L, TipoMovimientoCaja.EGRESO, "Gasto programado: Renta local", new BigDecimal("1500.00"));
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        GastoProgramado gasto = withId(GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2026-01-01T00:00:00Z")), 9L);
        when(gastoProgramadoRepository.findById(9L)).thenReturn(Optional.of(gasto));

        GastoProgramadoResumen desactivado = gastoProgramadoService.desactivar(1L, 9L);
        assertThat(desactivado.activo()).isFalse();

        GastoProgramadoResumen activado = gastoProgramadoService.activar(1L, 9L);
        assertThat(activado.activo()).isTrue();
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(gastoProgramadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gastoProgramadoService.actualizar(
                1L, 99L, "x", BigDecimal.ONE, FrecuenciaGasto.SEMANAL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private GastoProgramado withId(GastoProgramado gasto, Long id) {
        return new GastoProgramado(
                id, gasto.getTiendaId(), gasto.getConcepto(), gasto.getMonto(), gasto.getFrecuencia(),
                gasto.getProximaFecha(), gasto.isActivo(), gasto.getPagos());
    }
}
