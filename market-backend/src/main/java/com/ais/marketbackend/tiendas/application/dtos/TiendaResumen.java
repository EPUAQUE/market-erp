package com.ais.marketbackend.tiendas.application.dtos;

import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;

public record TiendaResumen(
        Long id, String codigo, String nombre, String direccion, String telefono, String correo,
        EstadoTienda estado) {
}
