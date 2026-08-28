package com.ais.marketbackend.grupostienda.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.impl.GrupoTiendaServiceImpl;
import com.ais.marketbackend.grupostienda.domain.exception.GrupoTiendaDuplicadoException;
import com.ais.marketbackend.grupostienda.domain.model.EstadoGrupoTienda;
import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrupoTiendaServiceImplTest {

    private GrupoTiendaRepository grupoTiendaRepository;
    private AutorizacionTiendaService autorizacionTiendaService;
    private GrupoTiendaServiceImpl grupoTiendaService;

    @BeforeEach
    void setUp() {
        grupoTiendaRepository = mock(GrupoTiendaRepository.class);
        autorizacionTiendaService = mock(AutorizacionTiendaService.class);
        grupoTiendaService = new GrupoTiendaServiceImpl(grupoTiendaRepository, autorizacionTiendaService);
        when(grupoTiendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(autorizacionTiendaService.grupoIdsPermitidas()).thenReturn(Optional.empty());
    }

    @Test
    void crearCanonicalizaElCodigoAMayusculas() {
        when(grupoTiendaRepository.existsByCodigo("PRINCIPAL")).thenReturn(false);

        GrupoTiendaResumen resumen = grupoTiendaService.crear("  principal  ", "Grupo Principal");

        assertThat(resumen.codigo()).isEqualTo("PRINCIPAL");
        assertThat(resumen.estado()).isEqualTo(EstadoGrupoTienda.ACTIVO);
    }

    @Test
    void crearConCodigoDuplicadoLanzaExcepcion() {
        when(grupoTiendaRepository.existsByCodigo("PRINCIPAL")).thenReturn(true);

        assertThatThrownBy(() -> grupoTiendaService.crear("principal", "Grupo Principal"))
                .isInstanceOf(GrupoTiendaDuplicadoException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(grupoTiendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> grupoTiendaService.actualizar(99L, "Nuevo nombre"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarYActivarDelegaEnElAgregado() {
        GrupoTienda grupoTienda = GrupoTienda.nuevo("PRINCIPAL", "Grupo Principal");
        when(grupoTiendaRepository.findById(1L)).thenReturn(Optional.of(grupoTienda));

        grupoTiendaService.desactivar(1L);
        assertThat(grupoTienda.estaActivo()).isFalse();

        grupoTiendaService.activar(1L);
        assertThat(grupoTienda.estaActivo()).isTrue();

        verify(grupoTiendaRepository, org.mockito.Mockito.times(2)).save(grupoTienda);
    }

    @Test
    void listarMapeaTodosLosGrupos() {
        when(grupoTiendaRepository.findAll()).thenReturn(java.util.List.of(
                GrupoTienda.nuevo("PRINCIPAL", "Grupo Principal")));

        var resultado = grupoTiendaService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("PRINCIPAL");
    }
}
