package com.ais.marketbackend.compras.domain.model;

/**
 * {@code RECIBIDA} es terminal: recibir una compra ya registró su kardex
 * (append-only en Inventario), así que no existe camino de vuelta a BORRADOR ni
 * a ANULADA — anular una compra ya recibida requeriría movimientos de reverso
 * explícitos, que este módulo no modela todavía.
 */
public enum EstadoCompra {
    BORRADOR,
    RECIBIDA,
    ANULADA
}
