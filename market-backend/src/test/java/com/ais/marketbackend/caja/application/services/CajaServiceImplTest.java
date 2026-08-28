package com.ais.marketbackend.caja.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.services.impl.CajaServiceImpl;
import com.ais.marketbackend.caja.domain.exception.CajaSesionAbiertaException;
import com.ais.marketbackend.caja.domain.exception.CorrelationIdReutilizadoException;
import com.ais.marketbackend.caja.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.caja.domain.repository.CajaSesionRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CajaServiceImplTest {

    private CajaSesionRepository cajaSesionRepository;
    private CajaServiceImpl cajaService;

    @BeforeEach
    void setUp() {
        cajaSesionRepository = mock(CajaSesionRepository.class);
        cajaService = new CajaServiceImpl(cajaSesionRepository);
        when(cajaSesionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void abrirConstruyeSesionAbierta() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());

        CajaSesionResumen resumen = cajaService.abrir(1L, new BigDecimal("100.00"));

        assertThat(resumen.estado()).isEqualTo(EstadoCajaSesion.ABIERTA);
        assertThat(resumen.saldoEsperado()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void abrirConSesionYaAbiertaLanzaExcepcion() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L))
                .thenReturn(Optional.of(CajaSesion.nueva(1L, BigDecimal.ZERO)));

        assertThatThrownBy(() -> cajaService.abrir(1L, new BigDecimal("100.00")))
                .isInstanceOf(CajaSesionAbiertaException.class);
    }

    @Test
    void registrarMovimientoSinCajaAbiertaLanzaNoEncontrada() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.registrarMovimiento(1L, TipoMovimientoCaja.INGRESO, "x", BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        when(cajaSesionRepository.findByTiendaId(1L, 0, 20)).thenReturn(new Pagina<>(List.of(sesion), 0, 20, 1, 1));

        Pagina<CajaSesionResumen> resultado = cajaService.listarPorTienda(1L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.totalElementos()).isEqualTo(1);
    }

    @Test
    void registrarMovimientoAcumulaEnLaSesionAbierta() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.of(sesion));

        CajaSesionResumen resumen =
                cajaService.registrarMovimiento(1L, TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("25.00"));

        assertThat(resumen.saldoEsperado()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    void cerrarSinCajaAbiertaLanzaNoEncontrada() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.cerrar(1L, BigDecimal.ZERO)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cerrarDejaLaSesionCerrada() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.of(sesion));

        CajaSesionResumen resumen = cajaService.cerrar(1L, new BigDecimal("100.00"));

        assertThat(resumen.estado()).isEqualTo(EstadoCajaSesion.CERRADA);
    }

    @Test
    void abrirConCorrelationIdYaExistenteYMismoMontoDevuelveLaSesionExistenteSinCrearOtra() {
        CajaSesion existente = CajaSesion.nueva(1L, new BigDecimal("100.00"), "corr-1");
        when(cajaSesionRepository.findByTiendaIdAndCorrelationIdApertura(1L, "corr-1"))
                .thenReturn(Optional.of(existente));

        CajaSesionResumen resumen = cajaService.abrir(1L, new BigDecimal("100.00"), "corr-1");

        assertThat(resumen.estado()).isEqualTo(EstadoCajaSesion.ABIERTA);
        verify(cajaSesionRepository, never()).findAbiertaByTiendaId(any());
        verify(cajaSesionRepository, never()).save(any());
    }

    @Test
    void abrirConCorrelationIdReutilizadoConMontoDistintoLanzaConflicto() {
        CajaSesion existente = CajaSesion.nueva(1L, new BigDecimal("100.00"), "corr-1");
        when(cajaSesionRepository.findByTiendaIdAndCorrelationIdApertura(1L, "corr-1"))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> cajaService.abrir(1L, new BigDecimal("999.00"), "corr-1"))
                .isInstanceOf(CorrelationIdReutilizadoException.class);
        verify(cajaSesionRepository, never()).save(any());
    }

    @Test
    void abrirConColisionDeInsercionConcurrenteReleeYDevuelveLaSesionIdempotente() {
        CajaSesion existente = CajaSesion.nueva(1L, new BigDecimal("100.00"), "corr-1");
        when(cajaSesionRepository.findByTiendaIdAndCorrelationIdApertura(1L, "corr-1"))
                .thenReturn(Optional.empty(), Optional.of(existente));
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());
        when(cajaSesionRepository.save(any())).thenThrow(new ReferenciaInvalidaException("colisión"));

        CajaSesionResumen resumen = cajaService.abrir(1L, new BigDecimal("100.00"), "corr-1");

        assertThat(resumen.estado()).isEqualTo(EstadoCajaSesion.ABIERTA);
    }

    @Test
    void registrarMovimientoConCorrelationIdYaExistenteYMismoContenidoNoDuplicaElMovimiento() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        sesion.registrarMovimiento(TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("25.00"), "corr-1");
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.of(sesion));

        CajaSesionResumen resumen = cajaService.registrarMovimiento(
                1L, TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("25.00"), "corr-1");

        assertThat(resumen.saldoEsperado()).isEqualByComparingTo(new BigDecimal("125.00"));
        verify(cajaSesionRepository, never()).save(any());
    }

    @Test
    void registrarMovimientoConCorrelationIdReutilizadoConMontoDistintoLanzaConflicto() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        sesion.registrarMovimiento(TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("25.00"), "corr-1");
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.of(sesion));

        assertThatThrownBy(() -> cajaService.registrarMovimiento(
                1L, TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("999.00"), "corr-1"))
                .isInstanceOf(CorrelationIdReutilizadoException.class);
        verify(cajaSesionRepository, never()).save(any());
    }

    @Test
    void cerrarConCorrelationIdYaExistenteEnCajaYaCerradaDevuelveLaSesionIdempotente() {
        CajaSesion cerrada = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        cerrada.cerrar(new BigDecimal("95.00"), "corr-1");
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());
        when(cajaSesionRepository.findByTiendaIdAndCorrelationIdCierre(1L, "corr-1"))
                .thenReturn(Optional.of(cerrada));

        CajaSesionResumen resumen = cajaService.cerrar(1L, new BigDecimal("95.00"), "corr-1");

        assertThat(resumen.estado()).isEqualTo(EstadoCajaSesion.CERRADA);
        verify(cajaSesionRepository, never()).save(any());
    }

    @Test
    void cerrarConCorrelationIdReutilizadoConMontoDistintoLanzaConflicto() {
        CajaSesion cerrada = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        cerrada.cerrar(new BigDecimal("95.00"), "corr-1");
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());
        when(cajaSesionRepository.findByTiendaIdAndCorrelationIdCierre(1L, "corr-1"))
                .thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> cajaService.cerrar(1L, new BigDecimal("999.00"), "corr-1"))
                .isInstanceOf(CorrelationIdReutilizadoException.class);
    }

    @Test
    void registrarMovimientoSiHayAbiertaDevuelveVacioSinCajaAbierta() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());

        Optional<CajaSesionResumen> resultado =
                cajaService.registrarMovimientoSiHayAbierta(1L, TipoMovimientoCaja.INGRESO, "Cobro", BigDecimal.TEN);

        assertThat(resultado).isEmpty();
    }

    @Test
    void registrarMovimientoSiHayAbiertaRegistraCuandoHaySesion() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.of(sesion));

        Optional<CajaSesionResumen> resultado =
                cajaService.registrarMovimientoSiHayAbierta(1L, TipoMovimientoCaja.INGRESO, "Cobro", new BigDecimal("10.00"));

        assertThat(resultado).isPresent();
        assertThat(resultado.get().saldoEsperado()).isEqualByComparingTo(new BigDecimal("110.00"));
    }

    @Test
    void hayAbiertaPorTiendaDevuelveFalsoSinCajaAbierta() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L)).thenReturn(Optional.empty());

        assertThat(cajaService.hayAbiertaPorTienda(1L)).isFalse();
    }

    @Test
    void hayAbiertaPorTiendaDevuelveVerdaderoConCajaAbierta() {
        when(cajaSesionRepository.findAbiertaByTiendaId(1L))
                .thenReturn(Optional.of(CajaSesion.nueva(1L, BigDecimal.ZERO)));

        assertThat(cajaService.hayAbiertaPorTienda(1L)).isTrue();
    }
}
