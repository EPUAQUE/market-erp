package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.adapters;

import com.ais.marketbackend.gastosprogramados.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import com.ais.marketbackend.gastosprogramados.domain.repository.GastoProgramadoRepository;
import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.mappers.GastoProgramadoEntityMapper;
import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.repositories.GastoProgramadoJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class GastoProgramadoRepositoryAdapter implements GastoProgramadoRepository {

    private final GastoProgramadoJpaRepository jpaRepository;
    private final GastoProgramadoEntityMapper mapper;

    public GastoProgramadoRepositoryAdapter(
            GastoProgramadoJpaRepository jpaRepository, GastoProgramadoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GastoProgramado save(GastoProgramado gasto) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(gasto)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda indicada no existe.");
        }
    }

    @Override
    public Optional<GastoProgramado> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GastoProgramado> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }
}
