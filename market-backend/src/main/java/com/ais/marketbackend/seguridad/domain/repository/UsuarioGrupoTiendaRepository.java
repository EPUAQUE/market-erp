package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import java.util.Collection;
import java.util.List;

public interface UsuarioGrupoTiendaRepository {

    UsuarioGrupoTienda save(UsuarioGrupoTienda usuarioGrupoTienda);

    List<UsuarioGrupoTienda> findByUsuarioId(Long usuarioId);

    /** Usado por Seguridad para filtrar el listado de usuarios por alcance del solicitante. */
    List<Long> listarUsuarioIdsPorGrupos(Collection<Long> grupoTiendaIds);
}
