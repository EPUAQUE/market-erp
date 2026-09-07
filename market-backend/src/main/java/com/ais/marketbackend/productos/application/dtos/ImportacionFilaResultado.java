package com.ais.marketbackend.productos.application.dtos;

/** Resultado de procesar una fila del Excel de carga masiva de productos. */
public record ImportacionFilaResultado(int fila, String codigoInterno, boolean creado, String mensaje) {
}
