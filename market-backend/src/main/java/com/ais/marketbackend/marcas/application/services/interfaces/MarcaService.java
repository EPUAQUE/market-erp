package com.ais.marketbackend.marcas.application.services.interfaces;

import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import java.util.List;

public interface MarcaService {

    MarcaResumen crear(String nombre);

    MarcaResumen actualizar(Long id, String nombre);

    List<MarcaResumen> listar();
}
