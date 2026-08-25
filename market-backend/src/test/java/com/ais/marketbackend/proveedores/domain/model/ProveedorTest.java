package com.ais.marketbackend.proveedores.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProveedorTest {

    @Test
    void nuevoProveedorEstaActivoPorDefecto() {
        Proveedor proveedor = Proveedor.nuevo("12345678-9", "Distribuidora XYZ", "Zona 1", "1234-5678", "xyz@market.demo");

        assertThat(proveedor.estaActivo()).isTrue();
        assertThat(proveedor.getEstado()).isEqualTo(EstadoProveedor.ACTIVO);
        assertThat(proveedor.getNit()).isEqualTo("12345678-9");
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        Proveedor proveedor = Proveedor.nuevo("12345678-9", "Distribuidora XYZ", null, null, null);

        proveedor.desactivar();
        assertThat(proveedor.estaActivo()).isFalse();

        proveedor.activar();
        assertThat(proveedor.estaActivo()).isTrue();
    }

    @Test
    void actualizarDatosNoCambiaElNit() {
        Proveedor proveedor = Proveedor.nuevo("12345678-9", "Distribuidora XYZ", "Zona 1", "1234-5678", "xyz@market.demo");

        proveedor.actualizarDatos("Distribuidora XYZ Renovada", "Zona 2", "8765-4321", "nueva@market.demo");

        assertThat(proveedor.getNit()).isEqualTo("12345678-9");
        assertThat(proveedor.getNombre()).isEqualTo("Distribuidora XYZ Renovada");
        assertThat(proveedor.getDireccion()).isEqualTo("Zona 2");
    }
}
