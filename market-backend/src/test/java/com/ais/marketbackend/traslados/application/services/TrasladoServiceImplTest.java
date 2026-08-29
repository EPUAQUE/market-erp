package com.ais.marketbackend.traslados.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.traslados.application.dtos.NuevaLineaTraslado;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import com.ais.marketbackend.traslados.application.services.impl.TrasladoServiceImpl;
import com.ais.marketbackend.traslados.domain.exception.EstadoTrasladoInvalidoException;
import com.ais.marketbackend.traslados.domain.model.LineaTraslado;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import com.ais.marketbackend.traslados.domain.repository.TrasladoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrasladoServiceImplTest {

    private TrasladoRepository trasladoRepository;
    private InventarioService inventarioService;
    private AutorizacionTiendaService autorizacionTiendaService;
    private TrasladoServiceImpl trasladoService;

    @BeforeEach
    void setUp() {
        trasladoRepository = mock(TrasladoRepository.class);
        inventarioService = mock(InventarioService.class);
        autorizacionTiendaService = mock(AutorizacionTiendaService.class);
        when(autorizacionTiendaService.tieneAcceso(any())).thenReturn(true);
        trasladoService = new TrasladoServiceImpl(trasladoRepository, inventarioService, autorizacionTiendaService);
        when(trasladoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConstruyeTrasladoEnBorrador() {
        TrasladoResumen resumen = trasladoService.crear(1L, 2L, List.of(new NuevaLineaTraslado(10L, BigDecimal.TEN)));

        assertThat(resumen.tiendaOrigenId()).isEqualTo(1L);
        assertThat(resumen.tiendaDestinoId()).isEqualTo(2L);
    }

    @Test
    void completarRegistraSalidaEnOrigenYEntradaEnDestinoConElCostoDeOrigen() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, new BigDecimal("5")))), 9L);
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));
        when(inventarioService.obtener(1L, 10L)).thenReturn(
                new InventarioResumen(1L, 1L, 10L, new BigDecimal("50.000"), new BigDecimal("6.0000")));

        trasladoService.completar(9L);

        verify(inventarioService).registrarMovimiento(
                1L, 10L, new BigDecimal("5"), new BigDecimal("6.0000"), TipoMovimiento.TRASLADO_SALIDA);
        verify(inventarioService).registrarMovimiento(
                2L, 10L, new BigDecimal("5"), new BigDecimal("6.0000"), TipoMovimiento.TRASLADO_ENTRADA);
    }

    @Test
    void completarConIdInexistenteLanzaNoEncontrado() {
        when(trasladoRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trasladoService.completar(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any());
    }

    @Test
    void completarCuandoOrigenNoTieneStockPropagaLaExcepcion() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.TEN))), 9L);
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));
        when(inventarioService.obtener(eq(1L), eq(10L))).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.ZERO, BigDecimal.ZERO));
        org.mockito.Mockito.doThrow(new StockInsuficienteException(10L, 1L))
                .when(inventarioService).registrarMovimiento(eq(1L), any(), any(), any(), eq(TipoMovimiento.TRASLADO_SALIDA));

        assertThatThrownBy(() -> trasladoService.completar(9L)).isInstanceOf(StockInsuficienteException.class);
        verify(inventarioService, never()).registrarMovimiento(eq(2L), any(), any(), any(), any());
    }

    @Test
    void completarCuandoDestinoNoPermiteIngresoPropagaLaExcepcion() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));
        when(inventarioService.obtener(eq(1L), eq(10L))).thenReturn(
                new InventarioResumen(1L, 1L, 10L, BigDecimal.TEN, BigDecimal.ONE));
        org.mockito.Mockito.doThrow(new MovimientoNoPermitidoException("no permitido"))
                .when(inventarioService).registrarMovimiento(eq(2L), any(), any(), any(), eq(TipoMovimiento.TRASLADO_ENTRADA));

        assertThatThrownBy(() -> trasladoService.completar(9L)).isInstanceOf(MovimientoNoPermitidoException.class);
    }

    @Test
    void anularConIdInexistenteLanzaNoEncontrado() {
        when(trasladoRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trasladoService.anular(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anularUnTrasladoYaCompletadoLanzaEstadoInvalido() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        traslado.completar();
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));

        assertThatThrownBy(() -> trasladoService.anular(9L)).isInstanceOf(EstadoTrasladoInvalidoException.class);
    }

    @Test
    void crearConTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAccesoATodas(any());

        assertThatThrownBy(() -> trasladoService.crear(1L, 2L, List.of(new NuevaLineaTraslado(10L, BigDecimal.TEN))))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(trasladoRepository, never()).save(any());
    }

    @Test
    void completarConTiendaFueraDeAlcanceLanzaNoEncontradoYNoTocaInventario() {
        // 404, no 403: completar() ya confirmó que el traslado existe
        // (obtenerORequerido) antes de chequear alcance — responder 403 ahí
        // distinguiría "no existe" de "existe pero no es mío" probando ids
        // (ver Javadoc de exigirAccesoOFingirNoEncontrado).
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAccesoATodas(any());

        assertThatThrownBy(() -> trasladoService.completar(9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any());
    }

    @Test
    void anularConTiendaFueraDeAlcanceLanzaNoEncontrado() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.findByIdConBloqueo(9L)).thenReturn(Optional.of(traslado));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAccesoATodas(any());

        assertThatThrownBy(() -> trasladoService.anular(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerConTiendaFueraDeAlcanceLanzaNoEncontrado() {
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.findById(9L)).thenReturn(Optional.of(traslado));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAccesoATodas(any());

        assertThatThrownBy(() -> trasladoService.obtener(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarFiltraLosTrasladosFueraDeAlcance() {
        Traslado permitido = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        Traslado fueraDeAlcance = withId(Traslado.nuevo(3L, 4L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 8L);
        when(trasladoRepository.findAll()).thenReturn(List.of(permitido, fueraDeAlcance));
        when(autorizacionTiendaService.tieneAcceso(1L)).thenReturn(true);
        when(autorizacionTiendaService.tieneAcceso(2L)).thenReturn(true);
        when(autorizacionTiendaService.tieneAcceso(3L)).thenReturn(false);
        when(autorizacionTiendaService.tieneAcceso(4L)).thenReturn(true);

        List<TrasladoResumen> resultado = trasladoService.listar();

        assertThat(resultado).extracting(TrasladoResumen::id).containsExactly(9L);
    }

    @Test
    void listarPaginadoConAlcanceGlobalDelegaEnListarSinFiltro() {
        when(autorizacionTiendaService.tiendaIdsPermitidas()).thenReturn(Optional.empty());
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.listar(0, 20)).thenReturn(new Pagina<>(List.of(traslado), 0, 20, 1, 1));

        Pagina<TrasladoResumen> resultado = trasladoService.listar(0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        verify(trasladoRepository, never()).listarPorTiendas(any(), anyInt(), anyInt());
    }

    @Test
    void listarPaginadoConAlcancePorTiendaDelegaEnListarPorTiendas() {
        when(autorizacionTiendaService.tiendaIdsPermitidas()).thenReturn(Optional.of(Set.of(1L)));
        Traslado traslado = withId(Traslado.nuevo(1L, 2L, List.of(LineaTraslado.nueva(10L, BigDecimal.ONE))), 9L);
        when(trasladoRepository.listarPorTiendas(Set.of(1L), 0, 20)).thenReturn(new Pagina<>(List.of(traslado), 0, 20, 1, 1));

        Pagina<TrasladoResumen> resultado = trasladoService.listar(0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        verify(trasladoRepository, never()).listar(0, 20);
    }

    private Traslado withId(Traslado traslado, Long id) {
        return new Traslado(
                id, traslado.getTiendaOrigenId(), traslado.getTiendaDestinoId(), traslado.getFecha(),
                traslado.getEstado(), traslado.getLineas());
    }
}
