package com.ais.marketbackend.marcas.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.impl.MarcaServiceImpl;
import com.ais.marketbackend.marcas.domain.exception.MarcaDuplicadaException;
import com.ais.marketbackend.marcas.domain.model.EstadoMarca;
import com.ais.marketbackend.marcas.domain.model.Marca;
import com.ais.marketbackend.marcas.domain.repository.MarcaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarcaServiceImplTest {

    private MarcaRepository marcaRepository;
    private MarcaServiceImpl marcaService;

    @BeforeEach
    void setUp() {
        marcaRepository = mock(MarcaRepository.class);
        marcaService = new MarcaServiceImpl(marcaRepository);
        when(marcaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearDevuelveElResumenCreado() {
        when(marcaRepository.existsByNombre("Nestlé")).thenReturn(false);

        MarcaResumen resumen = marcaService.crear("Nestlé");

        assertThat(resumen.nombre()).isEqualTo("Nestlé");
    }

    @Test
    void crearConNombreDuplicadoLanzaExcepcion() {
        when(marcaRepository.existsByNombre("Nestlé")).thenReturn(true);

        assertThatThrownBy(() -> marcaService.crear("Nestlé")).isInstanceOf(MarcaDuplicadaException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(marcaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marcaService.actualizar(99L, "Nueva"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarMapeaTodasLasMarcas() {
        when(marcaRepository.findAll()).thenReturn(java.util.List.of(Marca.nueva("Nestlé")));

        assertThat(marcaService.listar()).hasSize(1);
    }

    @Test
    void activarYDesactivarDelegaEnElAgregado() {
        Marca marca = Marca.nueva("Nestlé");
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));

        marcaService.desactivar(1L);
        assertThat(marca.getEstado()).isEqualTo(EstadoMarca.INACTIVA);

        marcaService.activar(1L);
        assertThat(marca.getEstado()).isEqualTo(EstadoMarca.ACTIVA);

        verify(marcaRepository, times(2)).save(marca);
    }
}
