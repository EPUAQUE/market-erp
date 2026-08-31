package com.ais.marketbackend.auditoria.infrastructure.persistence.adapters;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.auditoria.domain.repository.AuditEventRepository;
import com.ais.marketbackend.auditoria.infrastructure.persistence.mappers.AuditEventEntityMapper;
import com.ais.marketbackend.auditoria.infrastructure.persistence.repositories.AuditEventJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "fecha");

    private final AuditEventJpaRepository jpaRepository;
    private final AuditEventEntityMapper mapper;

    public AuditEventRepositoryAdapter(AuditEventJpaRepository jpaRepository, AuditEventEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AuditEvent save(AuditEvent evento) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(evento)));
    }

    @Override
    public Pagina<AuditEvent> listarTodo(int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findAll(PageRequest.of(pagina, tamano, MAS_RECIENTE_PRIMERO)).map(mapper::toDomain));
    }

    @Override
    public Pagina<AuditEvent> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository
                .findByTiendaId(tiendaId, PageRequest.of(pagina, tamano, MAS_RECIENTE_PRIMERO))
                .map(mapper::toDomain));
    }
}
