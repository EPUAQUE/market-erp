package com.ais.marketbackend.shared.api;

/**
 * Normaliza {@code page}/{@code size} recibidos por query param. {@code size} se
 * acota (no se rechaza) hasta {@link #TAMANO_MAXIMO} — generoso a propósito para que
 * un cliente que necesita "todo" (ej. el catálogo completo cacheado offline por el
 * POS) pueda pedirlo en una sola página sin implementar un loop de paginación.
 */
public final class PaginacionParams {

    public static final int TAMANO_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 5000;

    private PaginacionParams() {
    }

    public static int normalizarPagina(int page) {
        return Math.max(page, 0);
    }

    public static int normalizarTamano(int size) {
        return Math.min(Math.max(size, 1), TAMANO_MAXIMO);
    }
}
