package com.ais.marketbackend.ventas.domain.model;

/**
 * Cómo se pagó la venta, elegido por el vendedor al cerrarla. No cambia
 * ninguna regla de negocio de por sí — {@code CREDITO} sigue siendo "no
 * cobrar de inmediato" (ver {@code VentaServiceImpl}), el resto sigue
 * cobrándose completo al completar. Se persiste solo para reporting: antes de
 * este campo, el método de pago no existía en el backend y la app cliente lo
 * inferría del momento del cobro (ver CLAUDE.md de market-flutter).
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    CREDITO,
    MIXTO
}
