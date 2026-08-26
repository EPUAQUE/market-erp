package com.ais.marketbackend.seguridad.application.services.interfaces;

import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioTiendaResumen;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import java.util.List;

public interface UsuarioService {

    UsuarioResumen crear(String username, String passwordPlano, String nombre, String telefono, String correo);

    /** Usado por otros módulos (p. ej. Ventas) para resolver el usuario autenticado desde el JWT. */
    UsuarioResumen obtenerPorUsername(String username);

    void asignarTienda(Long usuarioId, Long tiendaId, Long rolId);

    List<UsuarioTiendaResumen> listarTiendas(Long usuarioId);

    PermisosEfectivos obtenerPermisosEfectivos(Long usuarioId);

    PermisosEfectivos obtenerPermisosEfectivosPorUsername(String username);

    List<UsuarioResumen> listar();
}
