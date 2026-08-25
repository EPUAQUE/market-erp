package com.ais.marketbackend.productos.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

/**
 * {@code precioVenta}/{@code stockMinimo}/{@code stockMaximo} viajan como
 * {@code String} (no como número JSON): así el frontend nunca los recibe como
 * {@code number} de JavaScript y evita perder precisión antes de pasarlos a
 * Decimal.js — mismo criterio que el resto del backoffice.
 */
@Value
@Builder
public class ProductoTiendaResponse {

    Long id;
    Long productoId;
    Long tiendaId;
    String precioVenta;
    String stockMinimo;
    String stockMaximo;
    boolean permitirVenta;
    boolean permitirIngreso;
    boolean activo;
}
