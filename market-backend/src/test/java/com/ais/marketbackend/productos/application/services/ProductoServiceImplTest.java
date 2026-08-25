package com.ais.marketbackend.productos.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.impl.ProductoServiceImpl;
import com.ais.marketbackend.productos.domain.exception.ProductoDuplicadoException;
import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.domain.repository.ProductoRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductoServiceImplTest {

    private ProductoRepository productoRepository;
    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        productoService = new ProductoServiceImpl(productoRepository);
        when(productoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listarPaginadoDelegaEnElRepositorioYMapeaElContenido() {
        Producto producto = Producto.nuevo("P001", null, "Leche", null, 1L, 2L, 3L, null);
        when(productoRepository.findAll(0, 20)).thenReturn(new Pagina<>(List.of(producto), 0, 20, 1, 1));

        Pagina<ProductoResumen> resultado = productoService.listar(0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).codigoInterno()).isEqualTo("P001");
    }

    @Test
    void crearDevuelveElResumenCreado() {
        when(productoRepository.existsByCodigoInterno("P001")).thenReturn(false);

        ProductoResumen resumen = productoService.crear("P001", "789", "Leche", "desc", 1L, 2L, 3L, null);

        assertThat(resumen.codigoInterno()).isEqualTo("P001");
        assertThat(resumen.activo()).isTrue();
    }

    @Test
    void crearConCodigoInternoDuplicadoLanzaExcepcion() {
        when(productoRepository.existsByCodigoInterno("P001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear("P001", null, "Leche", null, 1L, 2L, 3L, null))
                .isInstanceOf(ProductoDuplicadoException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(99L, null, "Leche", null, 1L, 2L, 3L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activarYDesactivarDelegaEnElAgregado() {
        Producto producto = Producto.nuevo("P001", null, "Leche", null, 1L, 2L, 3L, null);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        productoService.desactivar(1L);
        assertThat(producto.isActivo()).isFalse();

        productoService.activar(1L);
        assertThat(producto.isActivo()).isTrue();
    }
}
