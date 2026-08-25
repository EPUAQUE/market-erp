package com.ais.marketbackend.gastosprogramados.domain.model;

import com.ais.marketbackend.gastosprogramados.domain.exception.GastoInactivoException;
import com.ais.marketbackend.gastosprogramados.domain.exception.GastoNoVencidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gasto recurrente propio de una tienda (renta, servicios, planilla, etc.), sin
 * depender de una {@code Compra}. {@code generarPago} registra el pago del ciclo
 * vencido y avanza {@code proximaFecha} desde la fecha programada (no desde la
 * fecha real de pago), para que un pago tardío no corra el calendario.
 */
public class GastoProgramado {

    private final Long id;
    private final Long tiendaId;
    private String concepto;
    private BigDecimal monto;
    private FrecuenciaGasto frecuencia;
    private Instant proximaFecha;
    private boolean activo;
    private final List<PagoGastoProgramado> pagos;

    public GastoProgramado(
            Long id, Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia,
            Instant proximaFecha, boolean activo, List<PagoGastoProgramado> pagos) {
        this.id = id;
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.concepto = requerirConcepto(concepto);
        this.monto = requerirMontoValido(monto);
        this.frecuencia = Objects.requireNonNull(frecuencia, "frecuencia");
        this.proximaFecha = Objects.requireNonNull(proximaFecha, "proximaFecha");
        this.activo = activo;
        this.pagos = new ArrayList<>(Objects.requireNonNull(pagos, "pagos"));
    }

    public static GastoProgramado nuevo(
            Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia, Instant fechaInicio) {
        return new GastoProgramado(null, tiendaId, concepto, monto, frecuencia, fechaInicio, true, List.of());
    }

    public void actualizar(String concepto, BigDecimal monto, FrecuenciaGasto frecuencia) {
        this.concepto = requerirConcepto(concepto);
        this.monto = requerirMontoValido(monto);
        this.frecuencia = Objects.requireNonNull(frecuencia, "frecuencia");
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void generarPago(Instant ahora) {
        if (!activo) {
            throw new GastoInactivoException();
        }
        if (ahora.isBefore(proximaFecha)) {
            throw new GastoNoVencidoException();
        }
        this.pagos.add(PagoGastoProgramado.nuevo(ahora, monto));
        this.proximaFecha = proximaFecha.plus(frecuencia.getDias(), ChronoUnit.DAYS);
    }

    private static String requerirConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto no puede estar vacío.");
        }
        return concepto;
    }

    private static BigDecimal requerirMontoValido(BigDecimal monto) {
        Objects.requireNonNull(monto, "monto");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        return monto;
    }

    public Long getId() {
        return id;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public String getConcepto() {
        return concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public FrecuenciaGasto getFrecuencia() {
        return frecuencia;
    }

    public Instant getProximaFecha() {
        return proximaFecha;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<PagoGastoProgramado> getPagos() {
        return List.copyOf(pagos);
    }
}
