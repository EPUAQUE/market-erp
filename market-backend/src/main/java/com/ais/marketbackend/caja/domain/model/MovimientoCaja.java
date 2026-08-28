package com.ais.marketbackend.caja.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Entrada de caja (ingreso o egreso de efectivo). Vive dentro del agregado {@link CajaSesion}. */
public class MovimientoCaja {

    private final Long id;
    private final Instant fecha;
    private final TipoMovimientoCaja tipo;
    private final String concepto;
    private final BigDecimal monto;
    private final String correlationId;

    public MovimientoCaja(
            Long id, Instant fecha, TipoMovimientoCaja tipo, String concepto, BigDecimal monto,
            String correlationId) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.concepto = Objects.requireNonNull(concepto, "concepto");
        if (concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto es obligatorio.");
        }
        Objects.requireNonNull(monto, "monto");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        this.monto = monto;
        this.correlationId = correlationId;
    }

    public static MovimientoCaja nuevo(TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        return nuevo(tipo, concepto, monto, null);
    }

    public static MovimientoCaja nuevo(TipoMovimientoCaja tipo, String concepto, BigDecimal monto, String correlationId) {
        return new MovimientoCaja(null, Instant.now(), tipo, concepto, monto, correlationId);
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
