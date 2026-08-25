package com.ais.marketbackend.unidadesmedida.application.services.interfaces;

import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import java.util.List;

public interface UnidadMedidaService {

    UnidadMedidaResumen crear(String nombre, String abreviacion);

    UnidadMedidaResumen actualizar(Long id, String nombre, String abreviacion);

    List<UnidadMedidaResumen> listar();
}
