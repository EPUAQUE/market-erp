package com.ais.marketbackend.tiendas.application.services.interfaces;

import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import java.util.List;

public interface TiendaService {

    TiendaResumen crear(String codigo, String nombre, String direccion, String telefono, String correo);

    TiendaResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo);

    void activar(Long id);

    void desactivar(Long id);

    List<TiendaResumen> listar();
}
