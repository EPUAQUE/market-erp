package com.ais.marketbackend.cuentasporcobrar.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Abono recibido de un cliente sobre una cuenta por cobrar. Vive dentro del agregado {@link CuentaPorCobrar}. */
public class Cobro {

    private final Long id;
    private final Instant fecha;
    private final BigDecimal monto;
    private final MetodoPago metodoPago;

    public Cobro(Long id, Instant fecha, BigDecimal monto, MetodoPago metodoPago) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(monto, "monto");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del cobro debe ser mayor que cero.");
        }
        this.monto = monto;
        this.metodoPago = metodoPago;
    }

    public static Cobro nuevo(BigDecimal monto, MetodoPago metodoPago) {
        return new Cobro(null, Instant.now(), monto, metodoPago);
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

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }
}
