package com.ais.marketbackend.grupostienda.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GrupoTiendaTest {

    @Test
    void nuevoGrupoEstaActivoPorDefecto() {
        GrupoTienda grupoTienda = GrupoTienda.nuevo("PRINCIPAL", "Grupo Principal");

        assertThat(grupoTienda.estaActivo()).isTrue();
        assertThat(grupoTienda.getEstado()).isEqualTo(EstadoGrupoTienda.ACTIVO);
        assertThat(grupoTienda.getCodigo()).isEqualTo("PRINCIPAL");
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        GrupoTienda grupoTienda = GrupoTienda.nuevo("PRINCIPAL", "Grupo Principal");

        grupoTienda.desactivar();
        assertThat(grupoTienda.estaActivo()).isFalse();

        grupoTienda.activar();
        assertThat(grupoTienda.estaActivo()).isTrue();
    }

    @Test
    void actualizarDatosNoCambiaElCodigo() {
        GrupoTienda grupoTienda = GrupoTienda.nuevo("PRINCIPAL", "Grupo Principal");

        grupoTienda.actualizarDatos("Grupo Principal Renovado");

        assertThat(grupoTienda.getCodigo()).isEqualTo("PRINCIPAL");
        assertThat(grupoTienda.getNombre()).isEqualTo("Grupo Principal Renovado");
    }
}
