package com.ais.marketbackend.seguridad.domain.repository;

import java.util.List;

public interface PermisoRepository {

    boolean existsByCodigo(String codigo);

    List<String> findAllCodigos();
}
