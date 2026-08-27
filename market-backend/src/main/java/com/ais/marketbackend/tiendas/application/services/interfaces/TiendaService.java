package com.ais.marketbackend.tiendas.application.services.interfaces;

import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import java.util.List;

public interface TiendaService {

    TiendaResumen crear(String codigo, String nombre, String direccion, String telefono, String correo, Long grupoId);

    TiendaResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo, Long grupoId);

    void activar(Long id);

    void desactivar(Long id);

    List<TiendaResumen> listar();
}
