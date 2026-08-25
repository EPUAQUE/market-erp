package com.ais.marketbackend.tiendas.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.impl.TiendaServiceImpl;
import com.ais.marketbackend.tiendas.domain.exception.TiendaDuplicadaException;
import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;
import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TiendaServiceImplTest {

    private TiendaRepository tiendaRepository;
    private TiendaServiceImpl tiendaService;

    @BeforeEach
    void setUp() {
        tiendaRepository = mock(TiendaRepository.class);
        tiendaService = new TiendaServiceImpl(tiendaRepository);
        when(tiendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearCanonicalizaElCodigoAMayusculas() {
        when(tiendaRepository.existsByCodigo("CENTRAL")).thenReturn(false);

        TiendaResumen resumen = tiendaService.crear("  central  ", "Tienda Central", "Zona 1", "1234-5678", "c@x.com");

        assertThat(resumen.codigo()).isEqualTo("CENTRAL");
        assertThat(resumen.estado()).isEqualTo(EstadoTienda.ACTIVA);
    }

    @Test
    void crearConCodigoDuplicadoLanzaExcepcion() {
        when(tiendaRepository.existsByCodigo("CENTRAL")).thenReturn(true);

        assertThatThrownBy(() -> tiendaService.crear("central", "Tienda Central", null, null, null))
                .isInstanceOf(TiendaDuplicadaException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(tiendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tiendaService.actualizar(99L, "Nuevo nombre", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarYActivarDelegaEnElAgregado() {
        Tienda tienda = Tienda.nueva("CENTRAL", "Tienda Central", null, null, null);
        when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));

        tiendaService.desactivar(1L);
        assertThat(tienda.estaActiva()).isFalse();

        tiendaService.activar(1L);
        assertThat(tienda.estaActiva()).isTrue();

        verify(tiendaRepository, org.mockito.Mockito.times(2)).save(tienda);
    }

    @Test
    void listarMapeaTodasLasTiendas() {
        when(tiendaRepository.findAll()).thenReturn(java.util.List.of(
                Tienda.nueva("CENTRAL", "Tienda Central", null, null, null)));

        var resultado = tiendaService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("CENTRAL");
    }
}
