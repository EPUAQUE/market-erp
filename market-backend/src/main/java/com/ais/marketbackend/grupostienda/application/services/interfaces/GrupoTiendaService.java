package com.ais.marketbackend.grupostienda.application.services.interfaces;

import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import java.util.List;

public interface GrupoTiendaService {

    GrupoTiendaResumen crear(String codigo, String nombre);

    GrupoTiendaResumen actualizar(Long id, String nombre);

    void activar(Long id);

    void desactivar(Long id);

    List<GrupoTiendaResumen> listar();
}
