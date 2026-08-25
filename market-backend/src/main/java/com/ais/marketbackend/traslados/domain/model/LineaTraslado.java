package com.ais.marketbackend.traslados.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LineaTraslado {

    private final Long id;
    private final Long productoId;
    private final BigDecimal cantidad;

    public LineaTraslado(Long id, Long productoId, BigDecimal cantidad) {
        this.id = id;
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(cantidad, "cantidad");
        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        this.cantidad = cantidad;
    }

    public static LineaTraslado nueva(Long productoId, BigDecimal cantidad) {
        return new LineaTraslado(null, productoId, cantidad);
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
}
