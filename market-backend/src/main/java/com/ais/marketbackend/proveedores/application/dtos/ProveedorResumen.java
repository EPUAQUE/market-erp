package com.ais.marketbackend.proveedores.application.dtos;

import com.ais.marketbackend.proveedores.domain.model.EstadoProveedor;

public record ProveedorResumen(
        Long id, String nit, String nombre, String direccion, String telefono, String correo,
        EstadoProveedor estado) {
}
