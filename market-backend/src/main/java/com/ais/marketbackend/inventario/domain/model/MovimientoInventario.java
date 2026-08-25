package com.ais.marketbackend.inventario.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Entrada de kardex: append-only por diseño (ver trigger de PostgreSQL en la
 * migración) — nunca se actualiza ni se borra una vez registrada. Es el único
 * rastro auditable de cada cambio de existencia; {@link Inventario} solo guarda el
 * acumulado resultante.
 */
public class MovimientoInventario {

    private final Long id;
    private final Instant fecha;
    private final Long tiendaId;
    private final Long productoId;
    private final BigDecimal cantidad;
    private final BigDecimal costoUnitario;
    private final TipoMovimiento tipo;

    public MovimientoInventario(
            Long id, Instant fecha, Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
            TipoMovimiento tipo) {
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(costoUnitario, "costoUnitario");
        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que cero.");
        }
        if (costoUnitario.signum() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo.");
        }
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public static MovimientoInventario nuevo(
            Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario, TipoMovimiento tipo) {
        return new MovimientoInventario(null, Instant.now(), tiendaId, productoId, cantidad, costoUnitario, tipo);
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Long getTiendaId() {
        return tiendaId;
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

    public TipoMovimiento getTipo() {
        return tipo;
    }
}
