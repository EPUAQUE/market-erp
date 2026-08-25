package com.ais.marketbackend.gastosprogramados.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class PagoGastoProgramado {
    private final Long id;
    private final Instant fecha;
    private final BigDecimal monto;

    public PagoGastoProgramado(Long id, Instant fecha, BigDecimal monto) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.monto = Objects.requireNonNull(monto, "monto");
    }

    public static PagoGastoProgramado nuevo(Instant fecha, BigDecimal monto) {
        return new PagoGastoProgramado(null, fecha, monto);
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
