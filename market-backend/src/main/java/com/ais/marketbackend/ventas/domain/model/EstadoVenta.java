package com.ais.marketbackend.ventas.domain.model;

/**
 * {@code COMPLETADA} es terminal: completar una venta ya registró su salida de
 * kardex (append-only en Inventario), así que no hay camino de vuelta a
 * BORRADOR ni a ANULADA — mismo diseño que {@code EstadoCompra}.
 */
public enum EstadoVenta {
    BORRADOR,
    COMPLETADA,
    ANULADA
}
