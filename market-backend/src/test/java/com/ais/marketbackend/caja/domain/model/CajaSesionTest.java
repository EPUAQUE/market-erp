package com.ais.marketbackend.caja.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.caja.domain.exception.EstadoCajaSesionInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CajaSesionTest {

    @Test
    void nuevaEmpiezaAbiertaConSaldoEsperadoIgualAlMontoInicial() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));

        assertThat(sesion.getEstado()).isEqualTo(EstadoCajaSesion.ABIERTA);
        assertThat(sesion.saldoEsperado()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void registrarIngresoAumentaElSaldoEsperado() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));

        sesion.registrarMovimiento(TipoMovimientoCaja.INGRESO, "Cobro venta #1", new BigDecimal("50.00"));

        assertThat(sesion.saldoEsperado()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void registrarEgresoDisminuyeElSaldoEsperado() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));

        sesion.registrarMovimiento(TipoMovimientoCaja.EGRESO, "Pago proveedor", new BigDecimal("30.00"));

        assertThat(sesion.saldoEsperado()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void registrarMovimientoSobreCajaCerradaLanzaExcepcion() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        sesion.cerrar(new BigDecimal("100.00"));

        assertThatThrownBy(() -> sesion.registrarMovimiento(TipoMovimientoCaja.INGRESO, "x", BigDecimal.ONE))
                .isInstanceOf(EstadoCajaSesionInvalidoException.class);
    }

    @Test
    void cerrarTransicionaAEstadoCerradaYGuardaElMontoContado() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));

        sesion.cerrar(new BigDecimal("95.00"));

        assertThat(sesion.getEstado()).isEqualTo(EstadoCajaSesion.CERRADA);
        assertThat(sesion.getMontoFinalContado()).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(sesion.getFechaCierre()).isNotNull();
    }

    @Test
    void cerrarUnaCajaYaCerradaLanzaExcepcion() {
        CajaSesion sesion = CajaSesion.nueva(1L, new BigDecimal("100.00"));
        sesion.cerrar(new BigDecimal("100.00"));

        assertThatThrownBy(() -> sesion.cerrar(new BigDecimal("100.00")))
                .isInstanceOf(EstadoCajaSesionInvalidoException.class);
    }
}
