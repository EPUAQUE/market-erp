package com.ais.marketbackend.proveedores.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import com.ais.marketbackend.proveedores.application.services.impl.ProveedorServiceImpl;
import com.ais.marketbackend.proveedores.domain.exception.ProveedorDuplicadoException;
import com.ais.marketbackend.proveedores.domain.model.EstadoProveedor;
import com.ais.marketbackend.proveedores.domain.model.Proveedor;
import com.ais.marketbackend.proveedores.domain.repository.ProveedorRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProveedorServiceImplTest {

    private ProveedorRepository proveedorRepository;
    private ProveedorServiceImpl proveedorService;

    @BeforeEach
    void setUp() {
        proveedorRepository = mock(ProveedorRepository.class);
        proveedorService = new ProveedorServiceImpl(proveedorRepository);
        when(proveedorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearCanonicalizaElNitAMayusculas() {
        when(proveedorRepository.existsByNit("12345678-9")).thenReturn(false);

        ProveedorResumen resumen = proveedorService.crear(
                "  12345678-9  ", "Distribuidora XYZ", "Zona 1", "1234-5678", "c@x.com");

        assertThat(resumen.nit()).isEqualTo("12345678-9");
        assertThat(resumen.estado()).isEqualTo(EstadoProveedor.ACTIVO);
    }

    @Test
    void crearConNitDuplicadoLanzaExcepcion() {
        when(proveedorRepository.existsByNit("12345678-9")).thenReturn(true);

        assertThatThrownBy(() -> proveedorService.crear("12345678-9", "Distribuidora XYZ", null, null, null))
                .isInstanceOf(ProveedorDuplicadoException.class);
    }

    @Test
    void actualizarConIdInexistenteLanzaNoEncontrado() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorService.actualizar(99L, "Nuevo nombre", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarYActivarDelegaEnElAgregado() {
        Proveedor proveedor = Proveedor.nuevo("12345678-9", "Distribuidora XYZ", null, null, null);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        proveedorService.desactivar(1L);
        assertThat(proveedor.estaActivo()).isFalse();

        proveedorService.activar(1L);
        assertThat(proveedor.estaActivo()).isTrue();

        verify(proveedorRepository, org.mockito.Mockito.times(2)).save(proveedor);
    }

    @Test
    void listarMapeaTodosLosProveedores() {
        when(proveedorRepository.findAll()).thenReturn(java.util.List.of(
                Proveedor.nuevo("12345678-9", "Distribuidora XYZ", null, null, null)));

        var resultado = proveedorService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nit()).isEqualTo("12345678-9");
    }
}
