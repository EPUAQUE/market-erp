package com.ais.marketbackend.unidadesmedida.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.impl.UnidadMedidaServiceImpl;
import com.ais.marketbackend.unidadesmedida.domain.exception.UnidadMedidaDuplicadaException;
import com.ais.marketbackend.unidadesmedida.domain.model.UnidadMedida;
import com.ais.marketbackend.unidadesmedida.domain.repository.UnidadMedidaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnidadMedidaServiceImplTest {

    private UnidadMedidaRepository unidadMedidaRepository;
    private UnidadMedidaServiceImpl unidadMedidaService;

    @BeforeEach
    void setUp() {
        unidadMedidaRepository = mock(UnidadMedidaRepository.class);
        unidadMedidaService = new UnidadMedidaServiceImpl(unidadMedidaRepository);
        when(unidadMedidaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearDevuelveElResumenCreado() {
        when(unidadMedidaRepository.existsByNombre("Kilogramo")).thenReturn(false);

        UnidadMedidaResumen resumen = unidadMedidaService.crear("Kilogramo", "kg");

        assertThat(resumen.nombre()).isEqualTo("Kilogramo");
        assertThat(resumen.abreviacion()).isEqualTo("kg");
    }

    @Test
    void crearConNombreDuplicadoLanzaExcepcion() {
        when(unidadMedidaRepository.existsByNombre("Kilogramo")).thenReturn(true);

        assertThatThrownBy(() -> unidadMedidaService.crear("Kilogramo", "kg"))
                .isInstanceOf(UnidadMedidaDuplicadaException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(unidadMedidaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unidadMedidaService.actualizar(99L, "Litro", "l"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarModificaLaUnidadExistente() {
        UnidadMedida unidad = UnidadMedida.nueva("Kilogramo", "kg");
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidad));

        UnidadMedidaResumen resumen = unidadMedidaService.actualizar(1L, "Kilogramos", "Kg.");

        assertThat(resumen.nombre()).isEqualTo("Kilogramos");
        assertThat(resumen.abreviacion()).isEqualTo("Kg.");
    }

    @Test
    void listarMapeaTodasLasUnidades() {
        when(unidadMedidaRepository.findAll()).thenReturn(java.util.List.of(UnidadMedida.nueva("Kilogramo", "kg")));

        assertThat(unidadMedidaService.listar()).hasSize(1);
    }
}
