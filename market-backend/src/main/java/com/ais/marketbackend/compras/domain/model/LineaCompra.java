package com.ais.marketbackend.compras.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LineaCompra {

    private final Long id;
    private final Long productoId;
    private final BigDecimal cantidad;
    private final BigDecimal costoUnitario;

    public LineaCompra(Long id, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario) {
        this.id = id;
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(costoUnitario, "costoUnitario");
        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (cantidad.remainder(BigDecimal.ONE).signum() != 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número entero.");
        }
        if (costoUnitario.signum() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo.");
        }
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public static LineaCompra nueva(Long productoId, BigDecimal cantidad, BigDecimal costoUnitario) {
        return new LineaCompra(null, productoId, cantidad, costoUnitario);
    }

    public BigDecimal subtotal() {
        return cantidad.multiply(costoUnitario);
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }
}
