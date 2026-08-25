package com.ais.marketbackend.gastosprogramados.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.gastosprogramados.domain.exception.GastoInactivoException;
import com.ais.marketbackend.gastosprogramados.domain.exception.GastoNoVencidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class GastoProgramadoTest {

    @Test
    void nuevoEmpiezaActivoSinPagos() {
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, inicio);

        assertThat(gasto.isActivo()).isTrue();
        assertThat(gasto.getProximaFecha()).isEqualTo(inicio);
        assertThat(gasto.getPagos()).isEmpty();
    }

    @Test
    void generarPagoEnLaFechaVencidaRegistraElPagoYAvanzaLaProximaFecha() {
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, inicio);

        gasto.generarPago(inicio);

        assertThat(gasto.getPagos()).hasSize(1);
        assertThat(gasto.getPagos().get(0).getMonto()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(gasto.getProximaFecha()).isEqualTo(inicio.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void generarPagoAntesDeLaProximaFechaLanzaGastoNoVencido() {
        Instant inicio = Instant.parse("2026-06-01T00:00:00Z");
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, inicio);

        assertThatThrownBy(() -> gasto.generarPago(inicio.minus(1, ChronoUnit.DAYS)))
                .isInstanceOf(GastoNoVencidoException.class);
    }

    @Test
    void generarPagoSobreGastoInactivoLanzaGastoInactivo() {
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, inicio);
        gasto.desactivar();

        assertThatThrownBy(() -> gasto.generarPago(inicio)).isInstanceOf(GastoInactivoException.class);
    }

    @Test
    void actualizarCambiaConceptoMontoYFrecuencia() {
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2026-01-01T00:00:00Z"));

        gasto.actualizar("Renta bodega", new BigDecimal("1800.00"), FrecuenciaGasto.QUINCENAL);

        assertThat(gasto.getConcepto()).isEqualTo("Renta bodega");
        assertThat(gasto.getMonto()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(gasto.getFrecuencia()).isEqualTo(FrecuenciaGasto.QUINCENAL);
    }

    @Test
    void activarVuelveActivoUnGastoDesactivado() {
        GastoProgramado gasto = GastoProgramado.nuevo(1L, "Renta local", new BigDecimal("1500.00"),
                FrecuenciaGasto.MENSUAL, Instant.parse("2026-01-01T00:00:00Z"));
        gasto.desactivar();

        gasto.activar();

        assertThat(gasto.isActivo()).isTrue();
    }

    @Test
    void montoCeroOMenorEsInvalido() {
        assertThatThrownBy(() -> GastoProgramado.nuevo(1L, "Renta local", BigDecimal.ZERO,
                FrecuenciaGasto.MENSUAL, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conceptoVacioEsInvalido() {
        assertThatThrownBy(() -> GastoProgramado.nuevo(1L, " ", new BigDecimal("100.00"),
                FrecuenciaGasto.MENSUAL, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
