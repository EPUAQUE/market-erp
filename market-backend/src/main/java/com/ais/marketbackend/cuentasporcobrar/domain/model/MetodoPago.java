package com.ais.marketbackend.cuentasporcobrar.domain.model;

/**
 * Canal concreto por el que se recibió un {@link Cobro}. A diferencia de
 * {@code ventas.domain.model.MetodoPago} (la intención declarada al cerrar la
 * venta), este enum no tiene {@code CREDITO} ni {@code MIXTO} — un cobro
 * siempre es dinero recibido por un solo canal; "crédito" es la ausencia de
 * cobro y "mixto" es la suma de varios cobros, cada uno con su propio canal.
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA
}
