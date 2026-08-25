package com.ais.marketbackend.cuentasporpagar.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Abono aplicado a una cuenta por pagar. Vive dentro del agregado {@link CuentaPorPagar}, no es su propia raíz. */
public class Pago {

    private final Long id;
    private final Instant fecha;
    private final BigDecimal monto;

    public Pago(Long id, Instant fecha, BigDecimal monto) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(monto, "monto");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor que cero.");
        }
        this.monto = monto;
    }

    public static Pago nuevo(BigDecimal monto) {
        return new Pago(null, Instant.now(), monto);
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
