package com.ais.marketbackend.compras.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import com.ais.marketbackend.compras.application.services.impl.CompraServiceImpl;
import com.ais.marketbackend.compras.domain.exception.EstadoCompraInvalidoException;
import com.ais.marketbackend.compras.domain.model.Compra;
import com.ais.marketbackend.compras.domain.model.LineaCompra;
import com.ais.marketbackend.compras.domain.repository.CompraRepository;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompraServiceImplTest {

    private CompraRepository compraRepository;
    private InventarioService inventarioService;
    private CuentaPorPagarService cuentaPorPagarService;
    private CompraServiceImpl compraService;

    @BeforeEach
    void setUp() {
        compraRepository = mock(CompraRepository.class);
        inventarioService = mock(InventarioService.class);
        cuentaPorPagarService = mock(CuentaPorPagarService.class);
        compraService = new CompraServiceImpl(compraRepository, inventarioService, cuentaPorPagarService);
        when(compraRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConstruyeCompraEnBorrador() {
        CompraResumen resumen = compraService.crear(
                1L, 2L, List.of(new NuevaLineaCompra(1L, new BigDecimal("10"), new BigDecimal("5.00"))));

        assertThat(resumen.tiendaId()).isEqualTo(1L);
        assertThat(resumen.proveedorId()).isEqualTo(2L);
        assertThat(resumen.total()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void recibirRegistraUnMovimientoDeCompraPorLinea() {
        Compra compra = Compra.nueva(2L, 1L, List.of(
                LineaCompra.nueva(10L, new BigDecimal("10"), new BigDecimal("5.00")),
                LineaCompra.nueva(20L, new BigDecimal("3"), new BigDecimal("2.00"))));
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));

        compraService.recibir(1L, 5L);

        verify(inventarioService).registrarMovimiento(1L, 10L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA, 5L);
        verify(inventarioService).registrarMovimiento(1L, 20L, new BigDecimal("3"), new BigDecimal("2.00"), TipoMovimiento.COMPRA, 5L);
        verify(inventarioService, times(2)).registrarMovimiento(eq(1L), any(), any(), any(), eq(TipoMovimiento.COMPRA), eq(5L));
    }

    @Test
    void recibirCreaLaCuentaPorPagarDelProveedorPorElTotal() {
        Compra compra = Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, new BigDecimal("10"), new BigDecimal("5.00"))));
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));

        compraService.recibir(1L, 5L);

        verify(cuentaPorPagarService).crear(5L, 2L, 1L, new BigDecimal("50.00"));
    }

    @Test
    void recibirCuandoInventarioRechazaUnaLineaNoCreaLaCuentaPorPagar() {
        Compra compra = Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)));
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));
        org.mockito.Mockito.doThrow(new MovimientoNoPermitidoException("no permitido"))
                .when(inventarioService).registrarMovimiento(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> compraService.recibir(1L, 5L)).isInstanceOf(MovimientoNoPermitidoException.class);
        verify(cuentaPorPagarService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void recibirUnaCompraDeOtraTiendaLanzaNoEncontrada() {
        Compra compra = Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)));
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));

        assertThatThrownBy(() -> compraService.recibir(99L, 5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(inventarioService, never()).registrarMovimiento(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recibirCuandoInventarioRechazaUnaLineaPropagaLaExcepcion() {
        Compra compra = Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)));
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));
        org.mockito.Mockito.doThrow(new MovimientoNoPermitidoException("no permitido"))
                .when(inventarioService).registrarMovimiento(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> compraService.recibir(1L, 5L)).isInstanceOf(MovimientoNoPermitidoException.class);
    }

    @Test
    void anularConIdInexistenteLanzaNoEncontrado() {
        when(compraRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.anular(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anularUnaCompraYaRecibidaLanzaEstadoInvalido() {
        Compra compra = Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, BigDecimal.ONE, BigDecimal.ONE)));
        compra.recibir();
        when(compraRepository.findByIdConBloqueo(5L)).thenReturn(Optional.of(withId(compra, 5L, 1L)));

        assertThatThrownBy(() -> compraService.anular(1L, 5L)).isInstanceOf(EstadoCompraInvalidoException.class);
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        Compra compra = withId(
                Compra.nueva(2L, 1L, List.of(LineaCompra.nueva(10L, BigDecimal.ONE, BigDecimal.ONE))), 5L, 1L);
        when(compraRepository.findByTiendaId(1L, 0, 20)).thenReturn(new Pagina<>(List.of(compra), 0, 20, 1, 1));

        Pagina<CompraResumen> resultado = compraService.listarPorTienda(1L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).id()).isEqualTo(5L);
    }

    /** Simula lo que devolvería el repositorio tras persistir: mismos datos, con id asignado. */
    private Compra withId(Compra compra, Long id, Long tiendaId) {
        return new Compra(id, compra.getProveedorId(), tiendaId, compra.getFecha(), compra.getEstado(), compra.getLineas());
    }
}
