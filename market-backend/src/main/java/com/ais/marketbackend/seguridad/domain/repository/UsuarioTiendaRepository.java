package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import java.util.Collection;
import java.util.List;

public interface UsuarioTiendaRepository {

    UsuarioTienda save(UsuarioTienda usuarioTienda);

    List<UsuarioTienda> findByUsuarioId(Long usuarioId);

    /** Usado por Seguridad para filtrar el listado de usuarios por alcance del solicitante. */
    List<Long> listarUsuarioIdsPorTiendas(Collection<Long> tiendaIds);
}
