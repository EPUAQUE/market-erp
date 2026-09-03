package com.ais.marketbackend.unidadesmedida.application.dtos;

import com.ais.marketbackend.unidadesmedida.domain.model.EstadoUnidadMedida;

public record UnidadMedidaResumen(Long id, String nombre, String abreviacion, EstadoUnidadMedida estado) {
}
