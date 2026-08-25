package com.ais.marketbackend.clientes.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void nuevoClienteEstaActivoPorDefecto() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", "Zona 1", "1234-5678", "juan@market.demo", null);

        assertThat(cliente.estaActivo()).isTrue();
        assertThat(cliente.getEstado()).isEqualTo(EstadoCliente.ACTIVO);
        assertThat(cliente.getNit()).isEqualTo("12345678-9");
    }

    @Test
    void nuevoClienteSinNitEsValidoParaConsumidorFinal() {
        Cliente cliente = Cliente.nuevo(null, "Consumidor Final", null, null, null, null);

        assertThat(cliente.getNit()).isNull();
        assertThat(cliente.estaActivo()).isTrue();
    }

    @Test
    void nuevoClienteSinLimiteCreditoQuedaSinDefinir() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null);

        assertThat(cliente.getLimiteCredito()).isNull();
    }

    @Test
    void nuevoClienteConLimiteCreditoLoExpone() {
        Cliente cliente = Cliente.nuevo(
                "12345678-9", "Juan Pérez", null, null, null, new BigDecimal("5000.00"));

        assertThat(cliente.getLimiteCredito()).isEqualByComparingTo("5000.00");
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null);

        cliente.desactivar();
        assertThat(cliente.estaActivo()).isFalse();

        cliente.activar();
        assertThat(cliente.estaActivo()).isTrue();
    }

    @Test
    void actualizarDatosNoCambiaElNit() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", "Zona 1", "1234-5678", "juan@market.demo", null);

        cliente.actualizarDatos("Juan Pérez Actualizado", "Zona 2", "8765-4321", "nuevo@market.demo", null);

        assertThat(cliente.getNit()).isEqualTo("12345678-9");
        assertThat(cliente.getNombre()).isEqualTo("Juan Pérez Actualizado");
        assertThat(cliente.getDireccion()).isEqualTo("Zona 2");
    }

    @Test
    void actualizarDatosCambiaElLimiteCredito() {
        Cliente cliente = Cliente.nuevo("12345678-9", "Juan Pérez", null, null, null, null);

        cliente.actualizarDatos("Juan Pérez", null, null, null, new BigDecimal("1000.00"));

        assertThat(cliente.getLimiteCredito()).isEqualByComparingTo("1000.00");
    }
}
