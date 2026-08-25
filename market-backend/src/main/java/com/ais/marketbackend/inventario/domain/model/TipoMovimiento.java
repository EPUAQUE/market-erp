package com.ais.marketbackend.inventario.domain.model;

/**
 * Los ocho tipos de movimiento de kardex soportados. {@code esEntrada()} determina
 * si el movimiento incrementa existencia (y por tanto recalcula costo promedio) o
 * la decrementa — ver {@link Inventario#aplicar(MovimientoInventario)}.
 */
public enum TipoMovimiento {
    COMPRA,
    VENTA,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO,
    TRASLADO_ENTRADA,
    TRASLADO_SALIDA,
    DEVOLUCION_CLIENTE,
    DEVOLUCION_PROVEEDOR;

    public boolean esEntrada() {
        return this == COMPRA || this == AJUSTE_POSITIVO || this == TRASLADO_ENTRADA || this == DEVOLUCION_CLIENTE;
    }
}
