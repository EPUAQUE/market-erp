package com.ais.marketbackend.productos.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.impl.ProductoTiendaServiceImpl;
import com.ais.marketbackend.productos.domain.exception.ConfiguracionTiendaDuplicadaException;
import com.ais.marketbackend.productos.domain.model.ProductoTienda;
import com.ais.marketbackend.productos.domain.repository.ProductoTiendaRepository;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ProductoTiendaServiceImplTest {

    private ProductoTiendaRepository productoTiendaRepository;
    private AutorizacionTiendaService autorizacionTiendaService;
    private ProductoTiendaServiceImpl productoTiendaService;

    @BeforeEach
    void setUp() {
        productoTiendaRepository = mock(ProductoTiendaRepository.class);
        autorizacionTiendaService = mock(AutorizacionTiendaService.class);
        when(autorizacionTiendaService.tieneAcceso(any())).thenReturn(true);
        productoTiendaService = new ProductoTiendaServiceImpl(productoTiendaRepository, autorizacionTiendaService);
        when(productoTiendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void obtenerDevuelveVacioSiNoHayConfiguracion() {
        when(productoTiendaRepository.findByProductoIdAndTiendaId(1L, 1L)).thenReturn(Optional.empty());

        assertThat(productoTiendaService.obtener(1L, 1L)).isEmpty();
    }

    @Test
    void obtenerDevuelveResumenSiExisteConfiguracion() {
        ProductoTienda existente = ProductoTienda.nueva(1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findByProductoIdAndTiendaId(1L, 1L)).thenReturn(Optional.of(existente));

        assertThat(productoTiendaService.obtener(1L, 1L)).isPresent();
    }

    @Test
    void asignarCreaLaConfiguracionSiNoExiste() {
        when(productoTiendaRepository.findByProductoIdAndTiendaId(1L, 1L)).thenReturn(Optional.empty());

        ProductoTiendaResumen resumen = productoTiendaService.asignar(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("100"), true, true);

        assertThat(resumen.productoId()).isEqualTo(1L);
        assertThat(resumen.tiendaId()).isEqualTo(1L);
    }

    @Test
    void asignarConConfiguracionExistenteLanzaExcepcion() {
        ProductoTienda existente = ProductoTienda.nueva(1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findByProductoIdAndTiendaId(1L, 1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> productoTiendaService.asignar(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true))
                .isInstanceOf(ConfiguracionTiendaDuplicadaException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(productoTiendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoTiendaService.actualizar(
                99L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarYActivarDelegaEnElAgregado() {
        ProductoTienda pt = ProductoTienda.nueva(1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findById(1L)).thenReturn(Optional.of(pt));

        productoTiendaService.desactivar(1L);
        assertThat(pt.isActivo()).isFalse();

        productoTiendaService.activar(1L);
        assertThat(pt.isActivo()).isTrue();
    }

    @Test
    void asignarConTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(1L);

        assertThatThrownBy(() -> productoTiendaService.asignar(
                1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("100"), true, true))
                .isInstanceOf(AccessDeniedException.class);
        org.mockito.Mockito.verify(productoTiendaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void actualizarConTiendaFueraDeAlcanceLanzaNoEncontrado() {
        // 404, no 403: actualizar() ya confirmó que la configuración existe
        // (obtenerORequerida) antes de chequear alcance — responder 403 ahí
        // distinguiría "no existe" de "existe pero no es mía" probando ids
        // (ver Javadoc de exigirAccesoOFingirNoEncontrada).
        ProductoTienda pt = ProductoTienda.nueva(1L, 5L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findById(1L)).thenReturn(Optional.of(pt));
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(5L);

        assertThatThrownBy(() -> productoTiendaService.actualizar(
                1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activarConTiendaFueraDeAlcanceLanzaNoEncontrado() {
        ProductoTienda pt = ProductoTienda.nueva(1L, 5L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findById(1L)).thenReturn(Optional.of(pt));
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(5L);

        assertThatThrownBy(() -> productoTiendaService.activar(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarConTiendaFueraDeAlcanceLanzaNoEncontrado() {
        ProductoTienda pt = ProductoTienda.nueva(1L, 5L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findById(1L)).thenReturn(Optional.of(pt));
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(5L);

        assertThatThrownBy(() -> productoTiendaService.desactivar(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorTiendaPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        ProductoTienda pt = ProductoTienda.nueva(1L, 5L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findByTiendaId(5L, 0, 20)).thenReturn(new Pagina<>(List.of(pt), 0, 20, 1, 1));

        Pagina<ProductoTiendaResumen> resultado = productoTiendaService.listarPorTienda(5L, 0, 20);

        assertThat(resultado.contenido()).hasSize(1);
    }

    @Test
    void listarPorTiendaPaginadoConTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(9L);

        assertThatThrownBy(() -> productoTiendaService.listarPorTienda(9L, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listarPorTiendaConTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(9L);

        assertThatThrownBy(() -> productoTiendaService.listarPorTienda(9L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listarPorProductoFiltraLasTiendasFueraDeAlcance() {
        ProductoTienda permitida = ProductoTienda.nueva(1L, 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        ProductoTienda fueraDeAlcance = ProductoTienda.nueva(1L, 2L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true, true);
        when(productoTiendaRepository.findByProductoId(1L)).thenReturn(List.of(permitida, fueraDeAlcance));
        when(autorizacionTiendaService.tieneAcceso(1L)).thenReturn(true);
        when(autorizacionTiendaService.tieneAcceso(2L)).thenReturn(false);

        List<ProductoTiendaResumen> resultado = productoTiendaService.listarPorProducto(1L);

        assertThat(resultado).extracting(ProductoTiendaResumen::tiendaId).containsExactly(1L);
    }
}
