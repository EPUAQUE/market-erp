package com.ais.marketbackend.inventario.domain.model;

import com.ais.marketbackend.inventario.domain.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Acumulado de existencia y costo promedio ponderado de un producto en una
 * tienda. La única forma de mutarlo es {@link #aplicar(MovimientoInventario)} —
 * no hay setters de {@code existenciaActual}/{@code costoPromedioActual}: toda
 * modificación queda respaldada por el kardex append-only en
 * {@code MovimientoInventario}. Ver ARCHITECTURE.md §8.
 */
public class Inventario {

    private final Long id;
    private final Long tiendaId;
    private final Long productoId;
    private BigDecimal existenciaActual;
    private BigDecimal costoPromedioActual;

    public Inventario(
            Long id, Long tiendaId, Long productoId, BigDecimal existenciaActual, BigDecimal costoPromedioActual) {
        this.id = id;
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(existenciaActual, "existenciaActual");
        Objects.requireNonNull(costoPromedioActual, "costoPromedioActual");
        if (existenciaActual.signum() < 0) {
            throw new IllegalArgumentException("La existencia no puede ser negativa.");
        }
        if (costoPromedioActual.signum() < 0) {
            throw new IllegalArgumentException("El costo promedio no puede ser negativo.");
        }
        this.existenciaActual = existenciaActual;
        this.costoPromedioActual = costoPromedioActual;
    }

    public static Inventario nuevo(Long tiendaId, Long productoId) {
        return new Inventario(null, tiendaId, productoId, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public void aplicar(MovimientoInventario movimiento) {
        if (movimiento.getTipo().esEntrada()) {
            ingresar(movimiento.getCantidad(), movimiento.getCostoUnitario());
        } else {
            egresar(movimiento.getCantidad());
        }
    }

    private void ingresar(BigDecimal cantidad, BigDecimal costoUnitario) {
        BigDecimal valorActual = existenciaActual.multiply(costoPromedioActual);
        BigDecimal valorEntrante = cantidad.multiply(costoUnitario);
        BigDecimal nuevaExistencia = existenciaActual.add(cantidad);
        this.costoPromedioActual = valorActual.add(valorEntrante).divide(nuevaExistencia, 4, RoundingMode.HALF_UP);
        this.existenciaActual = nuevaExistencia;
    }

    private void egresar(BigDecimal cantidad) {
        if (existenciaActual.compareTo(cantidad) < 0) {
            throw new StockInsuficienteException(productoId, tiendaId);
        }
        this.existenciaActual = existenciaActual.subtract(cantidad);
    }

    public Long getId() {
        return id;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getExistenciaActual() {
        return existenciaActual;
    }

    public BigDecimal getCostoPromedioActual() {
        return costoPromedioActual;
    }
}
