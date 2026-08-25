package com.ais.marketbackend.categorias.application.services.interfaces;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import java.util.List;

public interface CategoriaService {

    CategoriaResumen crear(String nombre, String imagen);

    CategoriaResumen actualizar(Long id, String nombre, String imagen);

    void activar(Long id);

    void desactivar(Long id);

    List<CategoriaResumen> listar();
}
