package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import java.util.List;

public interface UsuarioGrupoTiendaRepository {

    UsuarioGrupoTienda save(UsuarioGrupoTienda usuarioGrupoTienda);

    List<UsuarioGrupoTienda> findByUsuarioId(Long usuarioId);
}
