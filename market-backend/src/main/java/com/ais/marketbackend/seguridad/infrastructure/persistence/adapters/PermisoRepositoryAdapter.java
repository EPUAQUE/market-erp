package com.ais.marketbackend.seguridad.infrastructure.persistence.adapters;

import com.ais.marketbackend.seguridad.domain.repository.PermisoRepository;
import com.ais.marketbackend.seguridad.infrastructure.persistence.repositories.PermisoJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PermisoRepositoryAdapter implements PermisoRepository {

    private final PermisoJpaRepository jpaRepository;

    public PermisoRepositoryAdapter(PermisoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpaRepository.existsByCodigo(codigo);
    }

    @Override
    public List<String> findAllCodigos() {
        return jpaRepository.findAllCodigos();
    }
}
