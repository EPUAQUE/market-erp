package com.ais.marketbackend.cuentasporcobrar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.cuentasporcobrar.domain.exception.CobroExcedeSaldoException;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CuentaConCobrosException;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.EstadoCuentaPorCobrarInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CuentaPorCobrarTest {

    @Test
    void nuevaEmpiezaPendienteConSaldoIgualAlMontoOriginal() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorCobrar.PENDIENTE);
        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(cuenta.getFechaVencimiento()).isAfter(cuenta.getFechaEmision());
    }

    @Test
    void registrarCobroParcialReduceElSaldoYQuedaPendiente() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.registrarCobro(new BigDecimal("40.00"), MetodoPago.EFECTIVO);

        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorCobrar.PENDIENTE);
        assertThat(cuenta.getCobros()).hasSize(1);
    }

    @Test
    void registrarCobroQueCubreElSaldoDejaLaCuentaCobrada() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.registrarCobro(new BigDecimal("100.00"), MetodoPago.EFECTIVO);

        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorCobrar.COBRADA);
    }

    @Test
    void registrarCobroMayorQueElSaldoLanzaExcepcion() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> cuenta.registrarCobro(new BigDecimal("100.01"), MetodoPago.EFECTIVO))
                .isInstanceOf(CobroExcedeSaldoException.class);
    }

    @Test
    void registrarCobroSobreCuentaCobradaLanzaEstadoInvalido() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.registrarCobro(new BigDecimal("100.00"), MetodoPago.EFECTIVO);

        assertThatThrownBy(() -> cuenta.registrarCobro(new BigDecimal("1.00"), MetodoPago.EFECTIVO))
                .isInstanceOf(EstadoCuentaPorCobrarInvalidoException.class);
    }

    @Test
    void anularSinCobrosDejaLaCuentaAnulada() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.anular();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorCobrar.ANULADA);
    }

    @Test
    void anularConCobrosYaRegistradosLanzaExcepcion() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.registrarCobro(new BigDecimal("10.00"), MetodoPago.EFECTIVO);

        assertThatThrownBy(cuenta::anular).isInstanceOf(CuentaConCobrosException.class);
    }

    @Test
    void anularUnaCuentaYaAnuladaLanzaEstadoInvalido() {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.anular();

        assertThatThrownBy(cuenta::anular).isInstanceOf(EstadoCuentaPorCobrarInvalidoException.class);
    }
}
