package com.ais.marketbackend.fel.infrastructure.persistence.adapters;

import com.ais.marketbackend.fel.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import com.ais.marketbackend.fel.domain.repository.DocumentoFelRepository;
import com.ais.marketbackend.fel.infrastructure.persistence.mappers.DocumentoFelEntityMapper;
import com.ais.marketbackend.fel.infrastructure.persistence.repositories.DocumentoFelJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class DocumentoFelRepositoryAdapter implements DocumentoFelRepository {

    private final DocumentoFelJpaRepository jpaRepository;
    private final DocumentoFelEntityMapper mapper;

    public DocumentoFelRepositoryAdapter(DocumentoFelJpaRepository jpaRepository, DocumentoFelEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DocumentoFel save(DocumentoFel documento) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(documento)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La venta o la tienda indicada no existe.");
        }
    }

    @Override
    public Optional<DocumentoFel> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DocumentoFel> findByVentaId(Long ventaId) {
        return jpaRepository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public List<DocumentoFel> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long siguienteNumero(Long tiendaId, String serie) {
        return jpaRepository.findMaxNumero(tiendaId, serie) + 1;
    }
}
