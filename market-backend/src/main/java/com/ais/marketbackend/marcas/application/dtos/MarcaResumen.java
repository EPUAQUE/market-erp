package com.ais.marketbackend.marcas.application.dtos;

import com.ais.marketbackend.marcas.domain.model.EstadoMarca;

public record MarcaResumen(Long id, String nombre, EstadoMarca estado) {
}
