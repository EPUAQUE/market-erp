package com.ais.marketbackend.clientes.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.impl.ClienteServiceImpl;
import com.ais.marketbackend.clientes.domain.exception.ClienteDuplicadoException;
import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.domain.repository.ClienteRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClienteServiceImplTest {

    private ClienteRepository clienteRepository;
    private ClienteServiceImpl clienteService;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        clienteService = new ClienteServiceImpl(clienteRepository);
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearCanonicalizaElNitAMayusculas() {
        when(clienteRepository.existsByNit("12345678-9")).thenReturn(false);

        ClienteResumen resumen = clienteService.crear("  12345678-9  ", "Juan Pérez", null, null, null, null);

        assertThat(resumen.nit()).isEqualTo("12345678-9");
    }

    @Test
    void crearSinNitNoConsultaDuplicados() {
        ClienteResumen resumen = clienteService.crear(null, "Consumidor Final", null, null, null, null);

        assertThat(resumen.nit()).isNull();
        verify(clienteRepository, never()).existsByNit(any());
    }

    @Test
    void crearConNitEnBlancoSeGuardaComoNuloSinConsultarDuplicados() {
        ClienteResumen resumen = clienteService.crear("   ", "Consumidor Final", null, null, null, null);

        assertThat(resumen.nit()).isNull();
        verify(clienteRepository, never()).existsByNit(any());
    }

    @Test
    void crearConNitDuplicadoLanzaExcepcion() {
        when(clienteRepository.existsByNit("12345678-9")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear("12345678-9", "Juan Pérez", null, null, null, null))
                .isInstanceOf(ClienteDuplicadoException.class);
    }

    @Test
    void crearConLimiteCreditoLoPersisteEnElResumen() {
        ClienteResumen resumen = clienteService.crear(
                null, "Juan Pérez", null, null, null, new BigDecimal("2500.00"));

        assertThat(resumen.limiteCredito()).isEqualByComparingTo("2500.00");
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(99L, "Nuevo nombre", null, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarCambiaElLimiteCredito() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResumen resumen = clienteService.actualizar(
                1L, "Juan Pérez", null, null, null, new BigDecimal("3000.00"));

        assertThat(resumen.limiteCredito()).isEqualByComparingTo("3000.00");
    }

    @Test
    void desactivarYActivarDelegaEnElAgregado() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.desactivar(1L);
        assertThat(cliente.estaActivo()).isFalse();

        clienteService.activar(1L);
        assertThat(cliente.estaActivo()).isTrue();
    }

    @Test
    void listarMapeaLaPaginaDeClientes() {
        when(clienteRepository.findAll(0, 20)).thenReturn(new Pagina<>(
                List.of(Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null)), 0, 20, 1, 1));

        var resultado = clienteService.listar(0, 20);

        assertThat(resultado.contenido()).hasSize(1);
        assertThat(resultado.contenido().get(0).nit()).isEqualTo("12345678-9");
    }
}
