package com.ais.marketbackend.grupostienda.application.dtos;

import com.ais.marketbackend.grupostienda.domain.model.EstadoGrupoTienda;

public record GrupoTiendaResumen(Long id, String codigo, String nombre, EstadoGrupoTienda estado) {
}
