package com.ais.marketbackend.inventario.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.impl.InventarioServiceImpl;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import com.ais.marketbackend.inventario.domain.model.Inventario;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.inventario.domain.repository.InventarioRepository;
import com.ais.marketbackend.inventario.domain.repository.MovimientoInventarioRepository;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.domain.model.MovimientoInventario;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class InventarioServiceImplTest {

    private InventarioRepository inventarioRepository;
    private MovimientoInventarioRepository movimientoInventarioRepository;
    private ProductoTiendaService productoTiendaService;
    private InventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        inventarioRepository = mock(InventarioRepository.class);
        movimientoInventarioRepository = mock(MovimientoInventarioRepository.class);
        productoTiendaService = mock(ProductoTiendaService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new InventarioServiceImpl(
                inventarioRepository, movimientoInventarioRepository, productoTiendaService, transactionManager);

        when(inventarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrarCompraSinConfiguracionLanzaNoEncontrada() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMovimiento(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarCompraConIngresoNoPermitidoLanzaExcepcion() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(true, false)));

        assertThatThrownBy(() -> service.registrarMovimiento(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA))
                .isInstanceOf(MovimientoNoPermitidoException.class);

        verify(inventarioRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void registrarVentaConVentaNoPermitidaLanzaExcepcion() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(false, true)));
        when(inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(1L, 2L))
                .thenReturn(Optional.of(Inventario.nuevo(1L, 2L)));

        assertThatThrownBy(() -> service.registrarMovimiento(
                1L, 2L, new BigDecimal("1"), new BigDecimal("5.00"), TipoMovimiento.VENTA))
                .isInstanceOf(MovimientoNoPermitidoException.class);
    }

    @Test
    void registrarCompraPermitidaActualizaInventarioYRegistraKardex() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(true, true)));
        when(inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(1L, 2L)).thenReturn(Optional.empty());

        InventarioResumen resultado = service.registrarMovimiento(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA);

        assertThat(resultado.existenciaActual()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(resultado.costoPromedioActual()).isEqualByComparingTo(new BigDecimal("5.0000"));
        verify(movimientoInventarioRepository).registrar(any());
    }

    @Test
    void registrarVentaSinExistenciaLanzaStockInsuficiente() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(true, true)));
        when(inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(1L, 2L))
                .thenReturn(Optional.of(Inventario.nuevo(1L, 2L)));

        assertThatThrownBy(() -> service.registrarMovimiento(
                1L, 2L, new BigDecimal("1"), new BigDecimal("5.00"), TipoMovimiento.VENTA))
                .isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void registrarMovimientoConColisionDeCreacionConcurrenteReintentaYActualizaLaFilaYaCreada() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(true, true)));
        when(inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(1L, 2L))
                .thenReturn(Optional.empty(), Optional.of(Inventario.nuevo(1L, 2L)));
        when(inventarioRepository.save(any()))
                .thenThrow(new ReferenciaInvalidaException("La tienda o el producto indicado no existe."))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventarioResumen resultado = service.registrarMovimiento(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA);

        assertThat(resultado.existenciaActual()).isEqualByComparingTo(new BigDecimal("10"));
        verify(inventarioRepository, org.mockito.Mockito.times(2)).save(any());
        verify(movimientoInventarioRepository).registrar(any());
    }

    @Test
    void registrarMovimientoConReferenciaRealmenteInvalidaPropagaTrasElReintento() {
        when(productoTiendaService.obtener(2L, 1L)).thenReturn(Optional.of(configuracion(true, true)));
        when(inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(1L, 2L)).thenReturn(Optional.empty());
        when(inventarioRepository.save(any()))
                .thenThrow(new ReferenciaInvalidaException("La tienda o el producto indicado no existe."));

        assertThatThrownBy(() -> service.registrarMovimiento(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA))
                .isInstanceOf(ReferenciaInvalidaException.class);

        verify(inventarioRepository, org.mockito.Mockito.times(2)).save(any());
        verify(movimientoInventarioRepository, org.mockito.Mockito.never()).registrar(any());
    }

    @Test
    void obtenerSinRegistroDevuelveResumenEnCero() {
        when(inventarioRepository.findByTiendaIdAndProductoId(1L, 2L)).thenReturn(Optional.empty());

        InventarioResumen resumen = service.obtener(1L, 2L);

        assertThat(resumen.existenciaActual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        when(inventarioRepository.findByTiendaId(1L, 0, 20))
                .thenReturn(new Pagina<>(List.of(Inventario.nuevo(1L, 2L)), 0, 20, 1, 1));

        Pagina<InventarioResumen> resultado = service.listarPorTienda(1L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void listarMovimientosPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        MovimientoInventario movimiento = MovimientoInventario.nuevo(
                1L, 2L, new BigDecimal("10"), new BigDecimal("5.00"), TipoMovimiento.COMPRA);
        when(movimientoInventarioRepository.findByTiendaIdAndProductoIdOrderByFechaDesc(1L, 2L, 0, 20))
                .thenReturn(new Pagina<>(List.of(movimiento), 0, 20, 1, 1));

        Pagina<MovimientoInventarioResumen> resultado = service.listarMovimientos(1L, 2L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
    }

    private ProductoTiendaResumen configuracion(boolean permitirVenta, boolean permitirIngreso) {
        return new ProductoTiendaResumen(
                1L, 2L, 1L, new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("100"), permitirVenta,
                permitirIngreso, true);
    }
}
