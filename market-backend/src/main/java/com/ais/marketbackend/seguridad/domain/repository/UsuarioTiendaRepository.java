package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import java.util.List;

public interface UsuarioTiendaRepository {

    UsuarioTienda save(UsuarioTienda usuarioTienda);

    List<UsuarioTienda> findByUsuarioId(Long usuarioId);
}
