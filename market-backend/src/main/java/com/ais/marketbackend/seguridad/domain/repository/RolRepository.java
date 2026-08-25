package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.Rol;
import java.util.List;
import java.util.Optional;

public interface RolRepository {

    Optional<Rol> findById(Long id);

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findAll();
}
