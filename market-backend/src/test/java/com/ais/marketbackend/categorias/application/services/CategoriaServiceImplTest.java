package com.ais.marketbackend.categorias.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.categorias.application.services.impl.CategoriaServiceImpl;
import com.ais.marketbackend.categorias.domain.exception.CategoriaDuplicadaException;
import com.ais.marketbackend.categorias.domain.model.Categoria;
import com.ais.marketbackend.categorias.domain.repository.CategoriaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoriaServiceImplTest {

    private CategoriaRepository categoriaRepository;
    private CategoriaServiceImpl categoriaService;

    @BeforeEach
    void setUp() {
        categoriaRepository = mock(CategoriaRepository.class);
        categoriaService = new CategoriaServiceImpl(categoriaRepository);
        when(categoriaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearConNombreDuplicadoLanzaExcepcion() {
        when(categoriaRepository.existsByNombre("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear("Bebidas", null))
                .isInstanceOf(CategoriaDuplicadaException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.actualizar(99L, "Bebidas", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activarYDesactivarDelegaEnElAgregado() {
        Categoria categoria = Categoria.nueva("Bebidas", null);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        categoriaService.desactivar(1L);
        assertThat(categoria.estaActiva()).isFalse();

        categoriaService.activar(1L);
        assertThat(categoria.estaActiva()).isTrue();

        verify(categoriaRepository, times(2)).save(categoria);
    }
}
