package com.ais.marketbackend.ventas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LineaVenta {

    private final Long id;
    private final Long productoId;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;

    public LineaVenta(Long id, Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
        this.id = id;
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(precioUnitario, "precioUnitario");
        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public static LineaVenta nueva(Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
        return new LineaVenta(null, productoId, cantidad, precioUnitario);
    }

    public BigDecimal subtotal() {
        return cantidad.multiply(precioUnitario);
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

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
