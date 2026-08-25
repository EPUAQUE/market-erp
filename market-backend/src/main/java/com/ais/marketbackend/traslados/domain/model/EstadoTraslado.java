package com.ais.marketbackend.traslados.domain.model;

/**
 * {@code COMPLETADO} es terminal: completar un traslado ya registró su salida
 * y entrada de kardex (append-only en Inventario, en ambas tiendas) — mismo
 * diseño que {@code EstadoCompra}/{@code EstadoVenta}.
 */
public enum EstadoTraslado {
    BORRADOR,
    COMPLETADO,
    ANULADO
}
