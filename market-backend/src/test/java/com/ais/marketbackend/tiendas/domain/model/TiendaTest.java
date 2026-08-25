package com.ais.marketbackend.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TiendaTest {

    @Test
    void nuevaTiendaEstaActivaPorDefecto() {
        Tienda tienda = Tienda.nueva("CENTRAL", "Tienda Central", "Zona 1", "1234-5678", "central@market.demo");

        assertThat(tienda.estaActiva()).isTrue();
        assertThat(tienda.getEstado()).isEqualTo(EstadoTienda.ACTIVA);
        assertThat(tienda.getCodigo()).isEqualTo("CENTRAL");
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        Tienda tienda = Tienda.nueva("CENTRAL", "Tienda Central", null, null, null);

        tienda.desactivar();
        assertThat(tienda.estaActiva()).isFalse();

        tienda.activar();
        assertThat(tienda.estaActiva()).isTrue();
    }

    @Test
    void actualizarDatosNoCambiaElCodigo() {
        Tienda tienda = Tienda.nueva("CENTRAL", "Tienda Central", "Zona 1", "1234-5678", "central@market.demo");

        tienda.actualizarDatos("Tienda Central Renovada", "Zona 2", "8765-4321", "nueva@market.demo");

        assertThat(tienda.getCodigo()).isEqualTo("CENTRAL");
        assertThat(tienda.getNombre()).isEqualTo("Tienda Central Renovada");
        assertThat(tienda.getDireccion()).isEqualTo("Zona 2");
    }
}
