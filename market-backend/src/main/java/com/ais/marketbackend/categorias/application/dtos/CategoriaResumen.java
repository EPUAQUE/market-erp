package com.ais.marketbackend.categorias.application.dtos;

import com.ais.marketbackend.categorias.domain.model.EstadoCategoria;

public record CategoriaResumen(Long id, String nombre, String imagen, EstadoCategoria estado) {
}
