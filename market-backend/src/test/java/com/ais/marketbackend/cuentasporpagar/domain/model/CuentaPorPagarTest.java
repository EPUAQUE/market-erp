package com.ais.marketbackend.cuentasporpagar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.cuentasporpagar.domain.exception.CuentaConPagosException;
import com.ais.marketbackend.cuentasporpagar.domain.exception.EstadoCuentaPorPagarInvalidoException;
import com.ais.marketbackend.cuentasporpagar.domain.exception.PagoExcedeSaldoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CuentaPorPagarTest {

    @Test
    void nuevaEmpiezaPendienteConSaldoIgualAlMontoOriginal() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(cuenta.getFechaVencimiento()).isAfter(cuenta.getFechaEmision());
    }

    @Test
    void registrarPagoParcialReduceElSaldoYQuedaPendiente() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.registrarPago(new BigDecimal("40.00"));

        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(cuenta.getPagos()).hasSize(1);
    }

    @Test
    void registrarPagoQueCubreElSaldoDejaLaCuentaPagada() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.registrarPago(new BigDecimal("100.00"));

        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
    }

    @Test
    void registrarPagoMayorQueElSaldoLanzaExcepcion() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> cuenta.registrarPago(new BigDecimal("100.01")))
                .isInstanceOf(PagoExcedeSaldoException.class);
    }

    @Test
    void registrarPagoSobreCuentaPagadaLanzaEstadoInvalido() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.registrarPago(new BigDecimal("100.00"));

        assertThatThrownBy(() -> cuenta.registrarPago(new BigDecimal("1.00")))
                .isInstanceOf(EstadoCuentaPorPagarInvalidoException.class);
    }

    @Test
    void anularSinPagosDejaLaCuentaAnulada() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));

        cuenta.anular();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
    }

    @Test
    void anularConPagosYaRegistradosLanzaExcepcion() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.registrarPago(new BigDecimal("10.00"));

        assertThatThrownBy(cuenta::anular).isInstanceOf(CuentaConPagosException.class);
    }

    @Test
    void anularUnaCuentaYaAnuladaLanzaEstadoInvalido() {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(1L, 2L, 3L, new BigDecimal("100.00"));
        cuenta.anular();

        assertThatThrownBy(cuenta::anular).isInstanceOf(EstadoCuentaPorPagarInvalidoException.class);
    }
}
